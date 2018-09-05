package com.alexandervanderzalm.controllers;

import com.alexandervanderzalm.Model.IPit;
import com.alexandervanderzalm.Model.IPitCollection;

public class IPitService {

    // IPitService.Drop(turn.collection, 2, 1)
    public static <T> void Drop(IPitCollection<IPit<T>> pitCollection, int index, T stones)
    {
        pitCollection.Get(index).Add(stones);
    }

    public static String DropTransformData(int index, int amount)
    {
        return String.format("Drop %s @ %s", index, amount);
    }

    //public static <T> void Grab

}