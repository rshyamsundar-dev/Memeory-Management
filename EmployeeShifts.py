import random
from collections import defaultdict, Counter
from typing import List, Dict, Tuple

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
SHIFTS = ["morning", "afternoon", "evening"]

class Employee:
    def __init__(self, name: str):
        self.name = name
        # preferences[day_index] = list of shifts in priority order, e.g. ["morning","evening"]
        self.preferences: Dict[int, List[str]] = {d: [] for d in range(7)}
        self.days_assigned = 0
        # assigned_shifts[day_index] = shift_name or None
        self.assigned_shifts: Dict[int, str] = {}

    def __repr__(self):
        return f"Employee({self.name}, assigned_days={self.days_assigned})"

def ask_user_input() -> Tuple[List[Employee], int]:
    """
    Interactive input helper. User can choose to enter employees manually or use sample data.
    Returns (employees_list, max_per_shift)
    """
    print("Employee Shift Scheduler — Input")
    use_sample = input("Use sample data? (y/n) [n]: ").strip().lower() or "n"
    employees = []
    if use_sample == "y":
        # Create sample employees with random preferences
        sample_names = ["Alice", "Bob", "Charlie", "Deepa", "Ethan", "Farah", "Gopal", "Hina", "Irfan"]
        employees = [Employee(n) for n in sample_names]
        for e in employees:
            for d in range(7):
                # create a random ranking of SHIFTS
                prefs = SHIFTS[:] 
                random.shuffle(prefs)
                # keep full ranking (bonus)
                e.preferences[d] = prefs
        print(f"Created {len(employees)} sample employees.")
    else:
        n = int(input("How many employees? Enter integer: ").strip())
        for i in range(n):
            name = input(f"Name of employee #{i+1}: ").strip() or f"Emp{i+1}"
            emp = Employee(name)
            print(f"Now enter preferences for {name}. For each day provide ranked shifts separated by '>' (e.g. morning>evening) or comma.")
            print("Use any subset of ['morning','afternoon','evening']. If empty, we treat as 'no preference'.")
            for d_idx, day in enumerate(DAYS):
                raw = input(f"  {day} preferences: ").strip()
                if not raw:
                    emp.preferences[d_idx] = []
                else:
                    # normalize input: separators '>' or ',' or whitespace
                    parts = [p.strip().lower() for p in raw.replace(">", ",").split(",") if p.strip()]
                    # filter invalid names and keep order
                    emp.preferences[d_idx] = [p for p in parts if p in SHIFTS]
            employees.append(emp)
    # max per shift
    max_per_shift_str = input("Enter maximum employees allowed per shift (per day). Default 5: ").strip()
    max_per_shift = int(max_per_shift_str) if max_per_shift_str.isdigit() else 5
    return employees, max_per_shift

def initialize_schedule():
    # schedule[day_idx][shift] = list of employee names assigned
    schedule = [ {s: [] for s in SHIFTS} for _ in range(7) ]
    return schedule

def can_assign(emp: Employee, day: int) -> bool:
    """Check if employee can be assigned on given day (not already assigned that day and under 5 days)."""
    if day in emp.assigned_shifts:
        return False
    if emp.days_assigned >= 5:
        return False
    return True

def assign_employee_to_shift(emp: Employee, day: int, shift: str, schedule: List[Dict[str,List[str]]]):
    schedule[day][shift].append(emp.name)
    emp.assigned_shifts[day] = shift
    emp.days_assigned += 1

def schedule_with_preferences(employees: List[Employee], max_per_shift: int):
    schedule = initialize_schedule()

    # Randomize employee order per day for fairness
    emp_order = employees[:]
    random.shuffle(emp_order)

    # First pass: try to assign employees to their top available preference for each day
    for day in range(7):
        random.shuffle(emp_order)
        for emp in emp_order:
            if not can_assign(emp, day):
                continue
            prefs = emp.preferences.get(day, []) or []
            assigned = False
            # try each preference in ranked order
            for shift in prefs:
                if len(schedule[day][shift]) < max_per_shift:
                    assign_employee_to_shift(emp, day, shift, schedule)
                    assigned = True
                    break
            # if no preferences or preferred full, we'll try in conflict resolution later (or now try other shifts same day)
            if not assigned:
                # attempt other shifts on same day (non-preferred) as early conflict resolution
                for shift in SHIFTS:
                    if shift in prefs:
                        continue
                    if len(schedule[day][shift]) < max_per_shift:
                        assign_employee_to_shift(emp, day, shift, schedule)
                        assigned = True
                        break
            # if still not assigned, leave unassigned for now (maybe next-day resolution)
    # After first pass, ensure min staffing per shift per day
    shortages = ensure_minimum_staffing(schedule, employees, min_required=2, max_per_shift=max_per_shift)

    # For employees still with free capacity (under 5 days), try to fill any remaining unassigned employees who are not assigned any shift on certain days
    # Also try to resolve any unfilled shift shortages by looking ahead next days if possible.
    if shortages:
        resolve_shortages_via_next_day(schedule, employees, shortages, max_per_shift)

    # Final check: try to assign unassigned employees (who still can work days) to any shift minimally to maximize coverage
    fill_remaining_with_available(schedule, employees, max_per_shift)

    return schedule

def ensure_minimum_staffing(schedule: List[Dict[str,List[str]]], employees: List[Employee], min_required: int, max_per_shift: int):
    """
    For each day & shift ensure at least min_required employees assigned.
    If not enough prefer that shift, randomly pick eligible employees who:
    - are not assigned that day
    - have days_assigned < 5
    Preference is given to those who haven't been assigned much.
    Returns a list of shortages as tuples (day, shift, missing_count).
    """
    shortages = []
    # Build a name->employee map
    emp_map = {e.name: e for e in employees}
    for day in range(7):
        for shift in SHIFTS:
            need = min_required - len(schedule[day][shift])
            if need > 0:
                # choose eligible employees
                eligible = [e for e in employees if can_assign(e, day)]
                # sort eligible by fewest days_assigned to spread load
                eligible.sort(key=lambda x: x.days_assigned)
                chosen = []
                for e in eligible:
                    if len(chosen) >= need:
                        break
                    # ensure we don't exceed max_per_shift
                    if len(schedule[day][shift]) + len(chosen) < max_per_shift:
                        chosen.append(e)
                for e in chosen:
                    assign_employee_to_shift(e, day, shift, schedule)
                short_remaining = need - len(chosen)
                if short_remaining > 0:
                    shortages.append((day, shift, short_remaining))
    return shortages

def resolve_shortages_via_next_day(schedule: List[Dict[str,List[str]]], employees: List[Employee], shortages: List[Tuple[int,str,int]], max_per_shift:int):
    """
    Try to resolve shortages by moving employees from other days (who have flexible preferences)
    or assigning employees to adjacent days/shifts if they have capacity.
    This is a heuristic: we try to find employees who are free on the shortage day and have capacity.
    """
    if not shortages:
        return []
    emp_map = {e.name: e for e in employees}
    updated_shortages = []
    for day, shift, need in shortages:
        chosen = []
        eligible = [e for e in employees if can_assign(e, day)]
        eligible.sort(key=lambda x: x.days_assigned)
        for e in eligible:
            if len(chosen) >= need:
                break
            if len(schedule[day][shift]) + len(chosen) < max_per_shift:
                assign_employee_to_shift(e, day, shift, schedule)
                chosen.append(e)
        still = need - len(chosen)
        if still > 0:
            # try next days: assign someone to next day but we need staffing for this specific day,
            # so next-day assignment doesn't fix today's shortage. We'll mark as unresolved.
            updated_shortages.append((day, shift, still))
    return updated_shortages

def fill_remaining_with_available(schedule: List[Dict[str,List[str]]], employees: List[Employee], max_per_shift: int):
    """
    Final pass: assign any employees who still have capacity to days where they were unassigned and shift has space.
    Tries to respect their preferences first.
    """
    emp_map = {e.name: e for e in employees}
    # For each day, for each employee unassigned that day and with capacity, try to assign to a space
    for day in range(7):
        for emp in employees:
            if not can_assign(emp, day):
                continue
            prefs = emp.preferences[day]
            assigned = False
            # try preferences first
            for shift in prefs:
                if len(schedule[day][shift]) < max_per_shift:
                    assign_employee_to_shift(emp, day, shift, schedule)
                    assigned = True
                    break
            if assigned:
                continue
            # try any shift
            for shift in SHIFTS:
                if len(schedule[day][shift]) < max_per_shift:
                    assign_employee_to_shift(emp, day, shift, schedule)
                    break

def pretty_print_schedule(schedule: List[Dict[str,List[str]]]):
    print("\nFinal Schedule for the Week:\n")
    for d_idx, day in enumerate(DAYS):
        print(f"{day}:")
        for shift in SHIFTS:
            names = schedule[d_idx][shift]
            print(f"  {shift.title():9}: {', '.join(names) if names else '(none)'}")
        print("-" * 40)

def summary_stats(schedule: List[Dict[str,List[str]]], employees: List[Employee]):
    # Count total assignments per employee and report anyone who exceeded constraints (shouldn't happen)
    counts = Counter()
    for d in range(7):
        for s in SHIFTS:
            for name in schedule[d][s]:
                counts[name] += 1
    print("\nEmployee assignment summary (days assigned):")
    for e in employees:
        print(f" - {e.name:12}: {e.days_assigned} day(s)")
    # Check for issues
    issues = []
    for e in employees:
        if e.days_assigned > 5:
            issues.append(f"{e.name} assigned > 5 days ({e.days_assigned})")
    # Check shifts for min staffing
    for d in range(7):
        for s in SHIFTS:
            if len(schedule[d][s]) < 2:
                issues.append(f"{DAYS[d]} {s} has fewer than 2 staff ({len(schedule[d][s])})")
    if issues:
        print("\nWarnings / Issues detected:")
        for it in issues:
            print(" -", it)
    else:
        print("\nNo issues detected. All constraints satisfied (or warnings handled).")

def main():
    employees, max_per_shift = ask_user_input()
    if not employees:
        print("No employees provided. Exiting.")
        return
    print("\nScheduling... (this may reshuffle employees for fairness)")
    schedule = schedule_with_preferences(employees, max_per_shift)
    pretty_print_schedule(schedule)
    summary_stats(schedule, employees)
    # Optionally export to file
    save = input("\nWould you like to save this schedule to 'schedule_week.txt'? (y/n) [n]: ").strip().lower() or "n"
    if save == "y":
        with open("schedule_week.txt", "w") as f:
            for d_idx, day in enumerate(DAYS):
                f.write(f"{day}:\n")
                for shift in SHIFTS:
                    names = schedule[d_idx][shift]
                    f.write(f"  {shift.title():9}: {', '.join(names) if names else '(none)'}\n")
                f.write("\n")
        print("Saved to schedule_week.txt")

if __name__ == "__main__":
    main()
