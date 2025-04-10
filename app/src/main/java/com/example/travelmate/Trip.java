package com.example.travelmate;

public class Trip {
    public String source;
    public String destination;
    public String date;
    public String tripId;

    public Trip(String source, String destination, String date, String tripId) {
        this.source = source;
        this.destination = destination;
        this.date = date;
        this.tripId = tripId;
    }
}
