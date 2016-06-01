package com.foofv.crawler.storage;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

@Entity(value = "animal", noClassnameStored = true)
class Animal {

    @Id
    String genus;
    boolean existed = true;

    public Animal() {

    }
}

@Entity(value = "animal", noClassnameStored = true)
class Mammal extends Animal {

    int normalTempreture = 37;

    {
        genus = "mammal";
    }
}

@Entity(value = "animal", noClassnameStored = true)
class Reptile extends Animal {

    int dormancyPeriod = 60;

    {
        genus = "mammal";
    }
}

class B {

    @Id
    String name = "B";
    @Reference
    List<Address> listB = new ArrayList<>();

    public B() {

    }

    public void addAddress(Address address) {
        listB.add(address);
    }
}

@Entity(value = "coll1", noClassnameStored = true)
class A {
    @Id
    String name = "A";
    // @Indexed(name = "name-idx", unique = true, dropDups = true)
    // int number = 250;
    @Reference
    List<Address> listA = new ArrayList<>();

    // @Reference
    Address address = new Address();

    public A() {

        // companyList.add("microsoft");
        // companyList.add("google");
    }

    public void addAddress(Address address) {
        listA.add(address);
    }
}

class Base<T, K> {

    Class<T> clazz;
    Class<K> clazz1;

    Base() {
        System.out.println(getClass().getTypeParameters().length);
        System.out.println(getClass().getTypeParameters()[0].getBounds()[0]);
//        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
//        clazz1 = (Class<K>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
//        System.out.println(clazz.getName());
//        System.out.println(clazz1.getName());
    }
}

public class ReflectTest {

    public static <T> T f(List<T> str) throws NoSuchMethodException {
        System.out.println(((ParameterizedType) ReflectTest.class.getMethod("f", List.class).getGenericParameterTypes()[0]).getActualTypeArguments()[0]);
        return null;
    }

    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, NoSuchMethodException {

//        Base<String, Integer> foo = new Base<String,Integer>();
        ReflectTest.<Integer>f(new ArrayList<Integer>());
    }

}
