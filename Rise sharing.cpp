#include <iostream>
#include <vector>
#include <memory>
#include <string>
#include <iomanip>

using namespace std;

// ----------------- Base class Ride -----------------
class Ride {
protected:
    string rideID;
    string pickupLocation;
    string dropoffLocation;
    double distance; // kilometers

public:
    Ride(const string& id="", const string& pickup="", const string& dropoff="", double dist=0.0)
        : rideID(id), pickupLocation(pickup), dropoffLocation(dropoff), distance(dist) {}

    virtual ~Ride() = default;

    // Encapsulation: use setters/getters rather than public fields
    void setRideID(const string& id) { rideID = id; }
    string getRideID() const { return rideID; }

    void setPickupLocation(const string& p) { pickupLocation = p; }
    string getPickupLocation() const { return pickupLocation; }

    void setDropoffLocation(const string& d) { dropoffLocation = d; }
    string getDropoffLocation() const { return dropoffLocation; }

    void setDistance(double d) { distance = d; }
    double getDistance() const { return distance; }

    // Polymorphic interface
    virtual double fare() const {
        // default base rate: 1.0 per km
        return distance * 1.0;
    }

    virtual void rideDetails() const {
        cout << "Ride " << rideID << "\n";
        cout << "  Pickup: " << pickupLocation << "\n";
        cout << "  Dropoff: " << dropoffLocation << "\n";
        cout << "  Distance: " << distance << " km\n";
        cout << "  Fare: " << fixed << setprecision(2) << fare() << "\n";
    }
};

// ----------------- StandardRide (inherits Ride) -----------------
class StandardRide : public Ride {
public:
    using Ride::Ride;
    double fare() const override {
        // standard rate
        return distance * 1.0;
    }
};

// ----------------- PremiumRide (inherits Ride) -----------------
class PremiumRide : public Ride {
public:
    using Ride::Ride;
    double fare() const override {
        double baseRate = 2.0;      // per km
        double bookingFee = 5.0;    // flat
        return distance * baseRate + bookingFee;
    }
};

// ----------------- Driver class -----------------
class Driver {
private:
    string driverID;
    string name;
    double rating;
    // Encapsulation: assignedRides is private and only mutated via addRide()
    vector<shared_ptr<Ride>> assignedRides;

public:
    Driver(const string& id="", const string& n="", double r=0.0)
        : driverID(id), name(n), rating(r) {}

    void setDriverID(const string& id) { driverID = id; }
    string getDriverID() const { return driverID; }

    void setName(const string& n) { name = n; }
    string getName() const { return name; }

    void setRating(double r) { rating = r; }
    double getRating() const { return rating; }

    void addRide(shared_ptr<Ride> ride) {
        assignedRides.push_back(ride);
    }

    // Return a copy of rides to avoid external mutation (encapsulation)
    vector<shared_ptr<Ride>> getAssignedRides() const {
        return assignedRides;
    }

    void getDriverInfo() const {
        cout << "Driver ID: " << driverID << "\n";
        cout << "Name: " << name << "\n";
        cout << "Rating: " << rating << "\n";
        cout << "Assigned rides: " << assignedRides.size() << "\n";
    }
};

// ----------------- Rider class -----------------
class Rider {
private:
    string riderID;
    string name;
    vector<shared_ptr<Ride>> requestedRides;

public:
    Rider(const string& id="", const string& n="") : riderID(id), name(n) {}

    void setRiderID(const string& id) { riderID = id; }
    string getRiderID() const { return riderID; }

    void setName(const string& n) { name = n; }
    string getName() const { return name; }

    void requestRide(shared_ptr<Ride> ride) {
        requestedRides.push_back(ride);
    }

    void viewRides() const {
        cout << "Rider: " << name << " - Ride history:\n";
        for (const auto& r : requestedRides) {
            r->rideDetails();
            cout << "-----------------\n";
        }
    }
};

// ----------------- Demo (polymorphism) -----------------
int main() {
    // Create rides (use smart pointers)
    shared_ptr<Ride> r1 = make_shared<StandardRide>("R100", "Station", "Mall", 4.5);
    shared_ptr<Ride> r2 = make_shared<PremiumRide>("R101", "Home", "Airport", 12.0);
    shared_ptr<Ride> r3 = make_shared<StandardRide>("R102", "Office", "Gym", 3.0);

    // Polymorphism: store different ride types in same vector
    vector<shared_ptr<Ride>> rides = { r1, r2, r3 };

    cout << "=== Polymorphic ride list ===\n";
    for (auto& ride : rides) {
        ride->rideDetails();               // virtual dispatch -> appropriate override
        cout << "Computed fare (polymorphic): " << ride->fare() << "\n\n";
    }

    // Driver and Rider demo
    Driver driver("D01", "Asha", 4.8);
    driver.addRide(r1);
    driver.addRide(r2);

    Rider rider("U01", "Rahul");
    rider.requestRide(r2);
    rider.requestRide(r3);

    cout << "=== Driver Info ===\n";
    driver.getDriverInfo();
    cout << "\n=== Rider View Rides ===\n";
    rider.viewRides();

    return 0;
}