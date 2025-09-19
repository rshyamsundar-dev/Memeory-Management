import java.util.*;
import java.util.stream.Collectors;

/**
 * Employee Shift Scheduling System
 * - Collects employee weekly preferences (primary + optional secondary)
 * - Schedules shifts for 7 days (MON..SUN) with Morning/Afternoon/Evening shifts
 * - Enforces constraints:
 *     - Max 1 shift per employee per day
 *     - Max 5 working days per employee per week
 *     - Minimum 2 employees per shift per day; fills randomly if needed
 * - Resolves conflicts when preferred shift is full by trying secondary or next day
 *
 * Assumptions:
 * - To create meaningful "full shift" conflicts, we use:
 *     MIN_PER_SHIFT = 2 (company requirement)
 *     MAX_PER_SHIFT = 3 (capacity per shift)  <-- change this if you want different behavior
 *
 * The program is interactive; enter 0 employees to run a built-in sample dataset.
 */
public class EmployeeScheduler {

    // Constants
    static final int MIN_PER_SHIFT = 2;
    static final int MAX_PER_SHIFT = 3; // capacity per shift (used to model "full" shift conflicts)
    static final int MAX_DAYS_PER_EMPLOYEE = 5;

    enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

        public static Day[] week() { return values(); }
    }

    enum Shift {
        MORNING, AFTERNOON, EVENING, NONE; // NONE means unavailable / no preference
        public static Shift fromChar(char c) {
            c = Character.toUpperCase(c);
            switch (c) {
                case 'M': return MORNING;
                case 'A': return AFTERNOON;
                case 'E': return EVENING;
                case 'N': return NONE;
                default: return NONE;
            }
        }
    }

    static class Preference {
        Shift primary;
        Shift secondary; // may be NONE if not provided

        Preference(Shift primary, Shift secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    static class Employee {
        String name;
        Map<Day, Preference> preferences = new EnumMap<>(Day.class);
        Map<Day, Shift> assigned = new EnumMap<>(Day.class);
        int assignedDays = 0;

        Employee(String name) { this.name = name; 
            for (Day d : Day.week()) assigned.put(d, Shift.NONE);
        }

        boolean isAssignedOn(Day d) {
            return assigned.get(d) != Shift.NONE;
        }

        void assign(Day d, Shift s) {
            if (assigned.get(d) == Shift.NONE && s != Shift.NONE) {
                assigned.put(d, s);
                assignedDays++;
            } else if (assigned.get(d) != Shift.NONE && s == Shift.NONE) {
                assigned.put(d, Shift.NONE);
            } else {
                assigned.put(d, s); // overwrite (generally not used)
            }
        }

        boolean canWorkMoreDays() {
            return assignedDays < MAX_DAYS_PER_EMPLOYEE;
        }

        @Override
        public String toString() { return name; }
    }

    static class Schedule {
        // Map: Day -> Shift -> List<Employee>
        Map<Day, Map<Shift, List<Employee>>> schedule = new EnumMap<>(Day.class);

        Schedule() {
            for (Day d : Day.week()) {
                Map<Shift, List<Employee>> m = new EnumMap<>(Shift.class);
                for (Shift s : new Shift[]{Shift.MORNING, Shift.AFTERNOON, Shift.EVENING})
                    m.put(s, new ArrayList<>());
                schedule.put(d, m);
            }
        }

        List<Employee> get(Day d, Shift s) {
            return schedule.get(d).get(s);
        }

        void add(Day d, Shift s, Employee e) {
            schedule.get(d).get(s).add(e);
            e.assign(d, s);
        }

        int count(Day d, Shift s) {
            return schedule.get(d).get(s).size();
        }

        String prettyPrint() {
            StringBuilder sb = new StringBuilder();
            for (Day d : Day.week()) {
                sb.append(d.name()).append(":\n");
                for (Shift s : new Shift[]{Shift.MORNING, Shift.AFTERNOON, Shift.EVENING}) {
                    sb.append("  ").append(s.name()).append(": ");
                    List<Employee> list = get(d,s);
                    if (list.isEmpty()) sb.append("[none]");
                    else sb.append(list.stream().map(emp -> emp.name).collect(Collectors.joining(", ")));
                    sb.append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    // Utility random
    static Random rand = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Employee Shift Scheduling System");
        System.out.println("--------------------------------");
        System.out.print("Enter number of employees (0 to run sample dataset): ");
        int n = readIntSafe(sc);

        List<Employee> employees = new ArrayList<>();

        if (n <= 0) {
            employees = sampleEmployees();
            System.out.println("\nRunning sample dataset with employees:");
            employees.forEach(e -> System.out.println(" - " + e.name));
            System.out.println();
        } else {
            for (int i = 0; i < n; ++i) {
                System.out.print("\nEnter name of employee #" + (i+1) + ": ");
                String name = sc.nextLine().trim();
                if (name.isEmpty()) { System.out.println("Name can't be empty. Try again."); i--; continue; }
                Employee emp = new Employee(name);
                for (Day d : Day.week()) {
                    System.out.println("For " + emp.name + ", day: " + d.name());
                    System.out.print("  Primary preference (M=morning, A=afternoon, E=evening, N=none): ");
                    char c = readCharSafe(sc);
                    Shift primary = Shift.fromChar(c);
                    System.out.print("  Secondary preference? (M/A/E/N) or press Enter for none: ");
                    String secLine = sc.nextLine().trim();
                    Shift secondary = Shift.NONE;
                    if (!secLine.isEmpty()) secondary = Shift.fromChar(secLine.charAt(0));
                    emp.preferences.put(d, new Preference(primary, secondary));
                }
                employees.add(emp);
            }
        }

        // If sample dataset, we may not have filled preferences; ensure each employee has preferences for each day
        fillMissingPreferencesWithRandom(employees);

        // Build schedule
        Schedule schedule = new Schedule();

        // 1st pass: try to assign employees to their primary preferences where possible (respecting MAX capacity and max days)
        for (Day d : Day.week()) {
            // For each shift, gather volunteers
            for (Shift s : new Shift[]{Shift.MORNING, Shift.AFTERNOON, Shift.EVENING}) {
                // gather employees who prefer this shift on this day and are available
                List<Employee> volunteers = employees.stream()
                        .filter(e -> !e.isAssignedOn(d)
                                && e.canWorkMoreDays()
                                && e.preferences.get(d) != null
                                && e.preferences.get(d).primary == s)
                        .collect(Collectors.toList());
                // assign up to MAX_PER_SHIFT from volunteers
                for (Employee e : volunteers) {
                    if (schedule.count(d,s) >= MAX_PER_SHIFT) {
                        // shift is full -> this creates a "conflict" for remaining volunteers
                        break;
                    }
                    schedule.add(d, s, e);
                }
            }
            // After primary assignments, ensure minimum per shift is met by filling from:
            //  - volunteers who had secondary preference for the shift
            //  - employees who are available and under 5 days
            for (Shift s : new Shift[]{Shift.MORNING, Shift.AFTERNOON, Shift.EVENING}) {
                if (schedule.count(d,s) < MIN_PER_SHIFT) {
                    // first pick secondary-preferring employees
                    List<Employee> secondaries = employees.stream()
                            .filter(e -> !e.isAssignedOn(d) && e.canWorkMoreDays()
                                    && e.preferences.get(d) != null
                                    && e.preferences.get(d).secondary == s)
                            .collect(Collectors.toList());
                    Collections.shuffle(secondaries, rand);
                    for (Employee e : secondaries) {
                        if (schedule.count(d,s) >= MIN_PER_SHIFT) break;
                        if (schedule.count(d,s) >= MAX_PER_SHIFT) break;
                        schedule.add(d,s,e);
                    }
                }
                // If still lacking, randomly pick any eligible employee who is not assigned that day and can work more days.
                if (schedule.count(d,s) < MIN_PER_SHIFT) {
                    List<Employee> pool = employees.stream()
                            .filter(e -> !e.isAssignedOn(d) && e.canWorkMoreDays())
                            .collect(Collectors.toList());
                    Collections.shuffle(pool, rand);
                    for (Employee e : pool) {
                        if (schedule.count(d,s) >= MIN_PER_SHIFT) break;
                        if (schedule.count(d,s) >= MAX_PER_SHIFT) break;
                        schedule.add(d,s,e);
                    }
                }
            }
        }

        // SECOND PASS: handle conflicts — employees who preferred a shift but couldn't be placed because shift became full earlier
        // For each employee and each day, if they are unassigned but had a primary preference (not NONE), try to place them:
        for (Day d : Day.week()) {
            for (Employee e : employees) {
                Preference pref = e.preferences.get(d);
                if (pref == null) continue;
                if (!e.isAssignedOn(d) && e.canWorkMoreDays() && pref.primary != Shift.NONE) {
                    // Try secondary on same day
                    boolean placed = false;
                    if (pref.secondary != Shift.NONE) {
                        Shift s = pref.secondary;
                        if (schedule.count(d,s) < MAX_PER_SHIFT) {
                            schedule.add(d,s,e);
                            placed = true;
                        }
                    }
                    // Try other shifts same day
                    if (!placed) {
                        for (Shift s : new Shift[]{Shift.MORNING, Shift.AFTERNOON, Shift.EVENING}) {
                            if (s == pref.primary) continue; // primary already full
                            if (schedule.count(d,s) < MAX_PER_SHIFT) {
                                schedule.add(d,s,e);
                                placed = true;
                                break;
                            }
                        }
                    }
                    // Try next day(s) primary then secondary
                    if (!placed) {
                        for (int offset=1; offset<7 && !placed; ++offset) {
                            Day next = Day.week()[(d.ordinal() + offset) % 7];
                            Preference npref = e.preferences.get(next);
                            if (npref != null && e.canWorkMoreDays() && !e.isAssignedOn(next)) {
                                // try primary on next day
                                if (npref.primary != Shift.NONE && schedule.count(next, npref.primary) < MAX_PER_SHIFT) {
                                    schedule.add(next, npref.primary, e);
                                    placed = true;
                                    break;
                                }
                                // try secondary
                                if (npref.secondary != Shift.NONE && schedule.count(next, npref.secondary) < MAX_PER_SHIFT) {
                                    schedule.add(next, npref.secondary, e);
                                    placed = true;
                                    break;
                                }
                                // try any shift next day
                                for (Shift s2 : new Shift[]{Shift.MORNING, Shift.AFTERNOON, Shift.EVENING}) {
                                    if (schedule.count(next, s2) < MAX_PER_SHIFT) {
                                        schedule.add(next, s2, e);
                                        placed = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // If still not placed, they will remain unassigned that day (possible if they've reached 5 days or all shifts full)
                }
            }
        }

        // AFTER SCHEDULING: ensure constraints satisfied; if some shift still below MIN_PER_SHIFT, try to fill by relaxing MAX_PER_SHIFT
        for (Day d : Day.week()) {
            for (Shift s : new Shift[]{Shift.MORNING, Shift.AFTERNOON, Shift.EVENING}) {
                if (schedule.count(d,s) < MIN_PER_SHIFT) {
                    // try to find employees who can work (even if it exceeds MAX_PER_SHIFT)
                    List<Employee> pool = employees.stream()
                            .filter(e -> !e.isAssignedOn(d) && e.canWorkMoreDays())
                            .collect(Collectors.toList());
                    Collections.shuffle(pool, rand);
                    for (Employee e : pool) {
                        if (schedule.count(d,s) >= MIN_PER_SHIFT) break;
                        schedule.add(d, s, e);
                    }
                }
            }
        }

        // Final output
        System.out.println("\nFinal Schedule for the Week:");
        System.out.println("============================");
        System.out.println(schedule.prettyPrint());

        // Print employee summaries (how many days assigned + day-by-day)
        System.out.println("Employee assignment summary:");
        for (Employee e : employees) {
            System.out.println("- " + e.name + " -> Assigned days: " + e.assignedDays);
            for (Day d : Day.week()) {
                Shift s = e.assigned.get(d);
                System.out.println("    " + d.name() + ": " + (s == Shift.NONE ? "OFF" : s.name()));
            }
            System.out.println();
        }

        sc.close();
    }

    // Helper: safe read int
    static int readIntSafe(Scanner sc) {
        while (true) {
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (Exception ex) {
                System.out.print("Please enter a valid integer: ");
            }
        }
    }

    static char readCharSafe(Scanner sc) {
        while (true) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) return 'N';
            return line.charAt(0);
        }
    }

    // Sample dataset (creates employees and reasonable preferences)
    static List<Employee> sampleEmployees() {
        List<Employee> list = new ArrayList<>();
        Employee a = new Employee("Alice");
        Employee b = new Employee("Bob");
        Employee c = new Employee("Carol");
        Employee d = new Employee("David");
        Employee e = new Employee("Eve");
        Employee f = new Employee("Frank");
        Employee g = new Employee("Grace");

        Collections.addAll(list, a,b,c,d,e,f,g);

        // Example: Alice prefers mornings all week
        for (Day day : Day.week()) a.preferences.put(day, new Preference(Shift.MORNING, Shift.AFTERNOON));
        // Bob prefers afternoons, but off Sundays
        for (Day day : Day.week()) {
            if (day == Day.SUNDAY) b.preferences.put(day, new Preference(Shift.NONE, Shift.NONE));
            else b.preferences.put(day,new Preference(Shift.AFTERNOON, Shift.EVENING));
        }
        // Carol prefers evening mon-fri, morning weekends
        for (Day day : Day.week()) {
            if (day.ordinal() <= Day.FRIDAY.ordinal()-1) // Monday-Friday
                c.preferences.put(day,new Preference(Shift.EVENING, Shift.AFTERNOON));
            else c.preferences.put(day,new Preference(Shift.MORNING, Shift.NONE));
        }
        // David mixed
        a: for (Day day : Day.week()) d.preferences.put(day, new Preference(randomShift(), Shift.NONE));
        // Eve prefers mornings mostly
        for (Day day : Day.week()) e.preferences.put(day, new Preference(Shift.MORNING, Shift.EVENING));
        // Frank prefers afternoon/ evening
        for (Day day : Day.week()) f.preferences.put(day, new Preference(Shift.AFTERNOON, Shift.MORNING));
        // Grace partially unavailable
        for (Day day : Day.week()) {
            if (day == Day.SATURDAY || day == Day.SUNDAY) gracePut(day,g,Shift.NONE,Shift.NONE);
            else gracePut(day,g,Shift.EVENING,Shift.AFTERNOON);
        }

        return list;
    }

    static void gracePut(Day d, Employee g, Shift p, Shift s) {
        g.preferences.put(d, new Preference(p,s));
    }

    static Shift randomShift() {
        int r = rand.nextInt(3);
        return r==0?Shift.MORNING: r==1?Shift.AFTERNOON:Shift.EVENING;
    }

    static void fillMissingPreferencesWithRandom(List<Employee> employees) {
        for (Employee e : employees) {
            for (Day d : Day.week()) {
                if (!e.preferences.containsKey(d)) {
                    // assign a random primary and secondary NONE
                    e.preferences.put(d, new Preference(randomShift(), Shift.NONE));
                }
            }
        }
    }
}
