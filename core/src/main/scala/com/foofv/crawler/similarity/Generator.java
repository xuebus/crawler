package com.foofv.crawler.similarity;

import com.foofv.crawler.parse.topic.entity.*;


import com.foofv.crawler.similarity.coordinate.BD09MCCoordinate;
import com.foofv.crawler.similarity.structure.Pair;
import com.foofv.crawler.similarity.structure.SimilarityObject;
import com.foofv.crawler.storage.MongoStorage;
import com.mongodb.DBRef;

import static com.foofv.crawler.similarity.literal.ProjectionConstant.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msfenn on 24/09/15.
 */
public class Generator {

    private static String[] fields = {TAKEOUT_MERCHANT_NAME, TAKEOUT_MERCHANT_ADDRESS, TAKEOUT_MERCHANT_LATITUDE, TAKEOUT_MERCHANT_LONGITUDE};

    private int baiduLimit;
    private int meituanLimit;

    private List<TakeoutMerchant> baiduList;
    private List<TakeoutMerchant> meituanList;

    public Generator(List<TakeoutMerchant> baiduList, List<TakeoutMerchant> meituanList) {

        this.baiduList = baiduList;
        this.meituanList = meituanList;
    }

    public Generator(int bdLimit, int mtLimit) {

        baiduLimit = bdLimit;
        meituanLimit = mtLimit;
    }

    public Pair<List<SimilarityObject>> generateSimilarityObjectListPair() {

        baiduList = MongoStorage.getListLimitedByValue(TakeoutMerchant.class, TAKEOUT_MERCHANT_ORIGIN_SITE, TAKEOUT_MERCHANT_BAIDU_FLAG, 0, baiduLimit, fields);
        meituanList = MongoStorage.getListLimitedByValue(TakeoutMerchant.class, TAKEOUT_MERCHANT_ORIGIN_SITE, TAKEOUT_MERCHANT_MEITUAN_FLAG, 0, meituanLimit, fields);

        Pair<List<SimilarityObject>> pair = null;
        if (baiduList.size() > 0 /*&& meituanList.size() > 0*/) {
            List<SimilarityObject> bdSimilarityObjectList = getSimilarityObjectList(baiduList, true);
            List<SimilarityObject> mtSimilarityObjectList = getSimilarityObjectList(meituanList, false);
            pair = new Pair<>(bdSimilarityObjectList, mtSimilarityObjectList);
        }

        return pair;
    }

    private List<SimilarityObject> getSimilarityObjectList(List<TakeoutMerchant> merchants, boolean isBaidu) {

        SimilarityObject similarityObject = null;
        List<SimilarityObject> similarityObjectList = new ArrayList<>();
        List<List<DBRef>> menuDBRefLists = null;
        List<DBRef> menuDBRefList = null;
        List<TakeoutMenu> menus = new ArrayList<>();
        for (TakeoutMerchant merchant : merchants) {
            similarityObject = new SimilarityObject();
            similarityObject.restaurantId = merchant.getObjectId();
            similarityObject.restaurantName = merchant.getName();
            similarityObject.address = merchant.getAddress();
            double lat = Double.parseDouble(merchant.getLatitude());
            double lng = Double.parseDouble(merchant.getLongitude());
            similarityObject.coordinate = new BD09MCCoordinate(lat, lng, true).toBD09LLCoordinate();

            menuDBRefList = MongoStorage.getDBReferenceListByKey(TakeoutMerchant.class, merchant.getObjectId(), TAKEOUT_MERCHANT_MENUS);
            menus.clear();
            for (DBRef dbRef : menuDBRefList) {
                menus.add(MongoStorage.getByKeyFiltered(TakeoutMenu.class, dbRef.getId(), TAKEOUT_MENU_MENU_NAME));
            }
//            List<Menu> menus = merchant.getMenus();
            if (menus.size() > 0 && isBaidu)
                menus.remove(0);//热销菜品
            Pair<List<String>> menuFoodListPair = getMenuFoodNameListPair(menus);
            similarityObject.menuNameList = menuFoodListPair._1();
            similarityObject.foodNameList = menuFoodListPair._2();
            similarityObjectList.add(similarityObject);
        }

        return similarityObjectList;
    }

    private Pair<List<String>> getMenuFoodNameListPair(List</*Menu*/TakeoutMenu> menuList) {

        Pair<List<String>> pair = null;
        List<DBRef> foodDBRefList = null;
        List<TakeoutFood> foods = new ArrayList<>();
        if (menuList.size() > 0) {
            List<String> menuNameList = new ArrayList<>();
            List<String> foodNameList = new ArrayList<>();
            pair = new Pair<>(menuNameList, foodNameList);
            for (Menu menu : menuList) {
                menuNameList.add(menu.getMenuName());
                foodDBRefList = MongoStorage.getDBReferenceListByKey(TakeoutMenu.class, menu.getObjectId(), TAKEOUT_MENU_FOODS);
                foods.clear();
                foodDBRefList.stream().forEach(dbRef -> foods.add(MongoStorage.getByKeyFiltered(TakeoutFood.class, dbRef.getId(), TAKEOUT_FOOD_FOOD_NAME)));
                foods.stream().forEach(food -> foodNameList.add(food.getFoodName()));
//                menu.getFoods().stream().forEach(food -> foodNameList.add(food.getFoodName()));
            }
        }

        return pair;
    }

    public static void main(String[] args) {

        Generator generator = new Generator(200, 100);
        Pair<List<SimilarityObject>> pair = generator.generateSimilarityObjectListPair();
    }
}
