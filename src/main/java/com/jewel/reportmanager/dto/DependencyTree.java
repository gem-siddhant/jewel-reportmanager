package com.jewel.reportmanager.dto;

import java.util.ArrayList;

public class DependencyTree {

    public Test data;
    public ArrayList<DependencyTree> child;

    public DependencyTree(Test data) {
        this.data = data;
        child = new ArrayList<>();
    }

}
