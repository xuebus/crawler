package com.foofv.crawler.storage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.types.ObjectId;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictor;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Query;

import com.mongodb.MongoClient;


class Company {
    @Id
    String name;
    int stuffs;
    int age;

    public Company() {
    }

    public Company(String name, int stuffs) {
        this.name = name;
        this.stuffs = stuffs;
    }
}

//@Entity(value = "coll1", noClassnameStored = true)
class Address {
    @Id
    // @Indexed(name = "country-idx", unique = true, dropDups = true)
            String country = "null";

    // Float number = 123.4f;
    @Reference(ignoreMissing = true)
    List<Company> company = new ArrayList<>();
    // List<MorphiaTest> list = new ArrayList<>();
    //
    // MorphiaTest test = new MorphiaTest();

    public Address() {

        // company.add("microsoft");
        // company.add("google");
        //
        // list.add(new MorphiaTest("test1", 1));
        // list.add(new MorphiaTest("test2", 2));
    }

    public Address(String country) {

        this.country = country;
    }
}

class User {
    @Id
    ObjectId id;
    @Reference(ignoreMissing = true)
    List<Address> address;
    String name;
    int age;

    public User() {

    }

    public User(String name, int age, List<Address> address) {

        this.name = name;
        this.age = age;
        this.address = address;
    }
}

public class MorphiaTest {

    public int nmb = 250;
    public String name = "test";

    public MorphiaTest() {
    }

    public MorphiaTest(String nm, int number) {
        name = nm;
        nmb = number;
    }

    public static void save(User user) {

        MongoClient client = new MongoClient("127.0.0.1", 27017);
        Morphia morphia = new Morphia();
        morphia.map(User.class);
        Datastore datastore = morphia.createDatastore(client, "test");
        for (Address address : user.address) {
            datastore.save(address);
        }
        datastore.save(user);
    }

    static <T> void f(T t) {

        System.out.println(t.getClass());
        System.out.println(t.getClass().getComponentType());
        if (t.getClass().isArray()) {
            System.out.println(Array.getLength(t));
            for (int i = 0; i < Array.getLength(t); ++i) {
                System.out.println(Array.get(t, i).getClass());
            }
        }
    }

    public static void main(String[] args) throws NoSuchFieldException {

        Object[] objects = new Object[3];
        objects[0] = "1";
        objects[1] = 1;
        objects[2] = new ArrayList<String>();
        f(objects);


//        MongoClient client = new MongoClient("127.0.0.1", 27017);
//        Morphia morphia = new Morphia();
//        morphia.map(User.class);
//        Datastore datastore = morphia.createDatastore(client, "test");
//        // List<String> list1 = new ArrayList<String>();
//        // List<String> list2 = new ArrayList<String>();
//        // list1.add("baller");
//        // list1.add("mellinda");
//        // list2.add("cook");
//        // list2.add("swell");
//        // datastore.save(list1);
//        // datastore.save(list2);
//        List<Address> list = new ArrayList<>();
//        Address address1 = new Address("USA");
//        Address address2 = new Address("China");
//        list.add(address1);
//        list.add(address2);
//        datastore.save(address1);
//        datastore.save(address2);
//        datastore.save(new User("bill", 60, list));
//        datastore.save(new User("jobs", 50, list));
//        Query<User> query = datastore.find(User.class, "name", "bill");
//        Iterator<User> userIterator = query.iterator();
//        while (userIterator.hasNext()) {
//            User user = userIterator.next();
//            System.out.println(user.id + " : " + user.name + " : " + user.age);
//            System.out.println(user.address.get(0).country + ": " + user.address.get(1).country);
//        }
    }
}
