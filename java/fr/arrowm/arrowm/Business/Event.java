package fr.arrowm.arrowm.Business;

/**
 * Class created to store all informations relative to an Event
 * Id : Used to retreive Event from DB
 * Name : Name of the event in French
 * RC : Bow used
 * Distance : Distance to the target
 * Spot : Type of spot used
 * Diameter : Diameter of the spot used
 * Shoot : Number of arrows between two shoot
 * Arrows by shoot : Number of shoot
 * Created by Gildas on 13/12/2016.
 */

public enum Event {
    C1840(0, "Salle", "Compound", 18, "FITA Tri-Spot", 40, 3, 10),
    C5080(1, "Federal", "Compound", 50, "FITA", 122, 6, 6),
    C50120(2, "FITA", "Compound", 50, "FITA Mono-Spot", 80, 6, 6),
    R1840(3, "Salle trispot", "Recurve", 18, "FITA Tri-Spot", 40, 3, 10),
    R184S(4, "Salle", "Recurve", 18, "FITA", 40, 3, 10),
    R5080(5, "Federal", "Recurve", 50, "FITA", 122, 6, 6),
    R50120(6, "FITA", "Recurve", 70, "FITA", 122, 6, 6);

    private int id = 0;
    private String name = "";
    private String rc = "";
    private int distance = 0;
    private String spot = "";
    private int diameter = 0;
    private int shoot = 0;
    private int arrowsByShoot = 0;


    Event(int id, String name, String rc, int distance, String spot, int diameter, int shoot, int arrowsByShoot) {
        this.id = id;
        this.name = name;
        this.rc = rc;
        this.distance = distance;
        this.spot = spot;
        this.diameter = diameter;
        this.shoot = shoot;
        this.arrowsByShoot = arrowsByShoot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRc() {
        return rc;
    }

    public String getSpot() {
        return spot;
    }

    public int getShoot() {
        return shoot;
    }

    public int getArrowsByShoot() {
        return arrowsByShoot;
    }

    public int getId() {
        return id;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return spot + " - Ø " + diameter + " cm";
    }
}
