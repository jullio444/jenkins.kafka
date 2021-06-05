package com.citi.gcg.eventhub.midas.kafka.stream.transformer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        int[] arr = {3,5,-4,8,11,1,-1,6};
        int[] arr1 = {12, 3, 1, 2, -6, 5, -8, 6}; //[-8, -6, 1, 2, 3, 5, 6, 12]
        int[] arr2 = {-1, -5, -10, -1100, -1100, -1101, -1102, -9001};
        int[] arr3 = {1,3,2};
        Arrays.sort(arr1);
        System.out.println(isMonotonic(arr3));
        //System.out.println(Arrays.toString(arr1));
        //System.out.println(Arrays.toString(test(arr, 91)));
    }

    public static void test2(int[] arr, int i, List<List<Integer>> result){
        //{3,5,-4,8,11,1,-1,6}
        //Arrays.sort(arr); //nlogn
        HashSet<Integer> set = new HashSet<>();
        for(int j=i+1; j<arr.length; j++) {
            int comp = -arr[i] - arr[j];
            if(set.contains((comp))){
                result.add(Arrays.asList(arr[i], arr[j], comp));
            }
        }

    }

    public static int[] test(int[] arr, int sum){
        //{3,5,-4,8,11,1,-1,6}
        Arrays.sort(arr); //nlogn
        for(int i=0, j=arr.length-1; i<arr.length && j>i;){
            int nsum = arr[i] + arr[j];
            if(nsum == sum)
                return new int[] {arr[i], arr[j]};
            if(nsum<sum){
                i++;
            }else if(nsum>sum){
                j--;
            }
        }
        return new int[] {};
    }

    public static boolean isMonotonic(int[] arr) {
        boolean increasing = true;
        boolean decreasing = true;
        for (int i = 0; i < arr.length - 1; ++i) {
            if (arr[i] > arr[i+1])
                increasing = false;
            if (arr[i] < arr[i+1])
                decreasing = false;
        }

        return increasing || decreasing;
    }
}
