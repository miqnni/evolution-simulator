package agh.ics.oop;

import agh.ics.oop.model.*;

import java.util.ArrayList;
import java.util.List;

public class World {
    public static void main(String[] args) {
        Settings testSettings = new Settings(
                5,
                5,
                0,
                7,
                0,
                -1,
                5,
                50,
                10,
                4,
                0,
                1,
                2,
                2,
                // mapType: 0 or 3
                3,
                50,
                0.0015);

        Simulation testSim = new Simulation(testSettings);
        testSim.run();

//        List<Animal> myList = new ArrayList<>();
//        myList.add(new Animal(new Vector2d(3,1), testSettings, 0));
//        myList.add(new Animal(new Vector2d(2,1), testSettings, 0));
//        System.out.println(myList.toString());

        //TODO
        //  interfejs
        //  mozliwosc zatrzymania symulacji
        //  mozliwosc klinkiecia i trackowania zwierzaka
        //  zapis wybranych ustawien (preset)
        //  mozliwosc odpalenia wielu symulacji naraz
        //  testy

    }
}
