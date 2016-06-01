package com.foofv.crawler.parse.xpath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NodePair {
    JsonNode parentNode;
    JsonNode node;

    NodePair(JsonNode parentNode, JsonNode node) {
        this.parentNode = parentNode;
        this.node = node;
    }
}

public class JsonParser {

    public static String convert2Chinese(String data) throws UnsupportedEncodingException {

        String regex = "\\\\u(\\w{4})";
        String result = "";
        Matcher matcher = Pattern.compile(regex).matcher(data);
        byte[] bytes = new byte[2];
        int lastPos = 0;
        int currPos = 0;
        while (matcher.find()) {
            int hex = Integer.parseInt(matcher.group(1), 16);
            bytes[0] = (byte) ((hex & 0xFF00) >> 8);
            bytes[1] = (byte) (hex & 0xFF);
            currPos = matcher.start();
            result += data.substring(lastPos, currPos);
            result += new String(bytes, "UTF-16");
            lastPos = matcher.end();
        }
        if (result.isEmpty())
            return data;
        else
            return result;
    }

    public static String getValueFromJson(String json, String key) throws IOException {

        return getValueFromJson(json, key, false);
    }

    public static String getValueFromJson(String json, String key, boolean isPedantic) throws IOException {

        String[] keys = key.split(":");
        NodePair nodePair = getNodeFromJson(json, keys, isPedantic);
        if (nodePair != null)
            return nodePair.node.toString();
        else
            return null;
    }

    public static List<String> getValuesFromJson(String json, String key) throws IOException {

        return getValuesFromJson(json, key, false);
    }

    public static List<String> getValuesFromJson(String json, String key, boolean isPedantic) throws IOException {

        List<String> values = new LinkedList<>();
        String[] keys = key.split(":");
        if (isPedantic) {
            NodePair nodePair = getNodeFromJson(json, keys, isPedantic);
            if (nodePair != null) {
                if (nodePair.parentNode.isArray()) {
                    JsonNode tmpNode;
                    for (int i = 0; i < nodePair.parentNode.size(); ++i) {
                        if ((tmpNode = nodePair.parentNode.get(i).get(keys[keys.length - 1])) != null)
                            values.add(formatValue(tmpNode.toString()));
                    }
                } else
                    values.add(formatValue(nodePair.node.toString()));
            }
        } else
            getValuesFromJson(json, keys, values);

        return values;
    }

    private static NodePair getNodeFromJson(String json, String[] keys, boolean isPedantic) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(json);
        JsonNode parentNode = null;
        int pathDepth = keys.length;
        int i = 0;
        JsonNode node = isPedantic ? rootNode.get(keys[i++]) : rootNode.findValue(keys[i++]);
        while (node != null && node.isContainerNode() && i < pathDepth) {
            parentNode = node;
            if (node.isArray() && isPedantic) {
                JsonNode tmpNode = node;
                for (int j = 0; j < node.size(); ++j) {
                    if ((tmpNode = node.get(j).get(keys[i])) != null) {
                        break;
                    }
                }
                node = tmpNode;
                ++i;
            } else
                node = isPedantic ? node.get(keys[i++]) : node.findValue(keys[i++]);
        }
        if (node != null && i == pathDepth)
            return new NodePair(parentNode, node);
        else
            return null;
    }

    private static void getValuesFromJson(String json, String[] keys, List<String> values) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(json);
        getValuesFromJson(rootNode, keys, 0, values);
    }

    private static void getValuesFromJson(JsonNode parentNode, String[] keys, int depth, List<String> values) {

        int pathDepth = keys.length;
        List<JsonNode> nodes = parentNode.findValues(keys[depth]);
        for (JsonNode node : nodes) {
            if (node != null && node.isContainerNode() && depth < pathDepth - 1)
                getValuesFromJson(node, keys, depth + 1, values);
            else if (node != null && depth == pathDepth - 1)
                values.add(formatValue(node.toString()));
        }
    }

    private static String formatValue(String value) {

        if (value.startsWith("\""))
            value = value.substring(1, value.length() - 1);
        return value;
    }

    public static void main(String args[]) throws IOException {


//		String html = "<p>An -<a id='no1' href='http://example.com/'><b>example</b></a> link.</p>";
//		Document doc = Jsoup.parse(html);// 解析HTML字符串返回一个Document实现
//		String text = doc/* .body() */.text(); // "An example link"//取得字符串中的文本
//		System.out.println(text);
//
//		Element link = doc.select("a[href]").first();// 查找第一个a元素
//		String linkHref = link.attr("href"); // "http://example.com/"//取得链接地址
//		System.out.println(linkHref);
//		String linkText = link.text(); // "example""//取得链接地址中的文本
//		System.out.println(linkText);
//
//		String linkOuterH = link.outerHtml();
//		System.out.println(linkOuterH);
//		// "<a href="http://example.com"><b>example</b></a>"
//		String linkInnerH = link.html(); // "<b>example</b>"//取得链接内的html内容
//		System.out.println(linkInnerH);
//
//		System.out.println(doc.childNodeSize());
//		System.out.println(doc.body().children().get(0).textNodes());
//
//		System.out.println(doc.body().html());
//		System.out.println(doc.getElementsByTag("body").html());
//
//		Element tag = doc.select("p a b").first();// 查找第一个a元素
//		System.out.println(tag.outerHtml());
//
//		Document doc1 = Jsoup.connect("http://www.baidu.com/").get();
//		String title = doc1.title();
//		System.out.println(title);

//        String json = "{\"no\":\"1\"," +
//                "\"result\":{" +
//                "\"info\":\"yes\", \"array\":[{\"foo\":\"bar\"},{\"foo\":\"biz\"}]" +
//                "}," + "\"result1\":{" + "\"info\":\"yes\", \"array\":[{\"foo\":\"bar1\"},{\"foo\":\"biz1\"}]}" +
//                ",\"array\":[{\"foo\":\"bar2\"},{\"foo\":\"biz2\"}]" +
//                "}";
//        String json1 = "{\"array\":[{\"foo\":\"bar\"},{\"foo\":\"biz\"}]}";
//        System.out.println(rootNode./*get("result").*/get("array").getNodeType());
//        System.out.println(rootNode./*get("result").*/get("array").isObject());
//        System.out.println(rootNode./*get("result").*/get("array").get("fo"));
//        System.out.println(rootNode./*get("result").*/get("array").get(0).get("foo"));
//        System.out.println(rootNode./*get("result").*/get("array").get(1).get("foo"));
//        System.out.println(getValuesFromJson(json1, "array:foo", true));
//        System.out.println(getValueFromJson(json1, "array:foo", true));
//        System.out.println(getValueFromJson(json, "result:array", false));
//        System.out.println(getValueFromJson(json, "result:array:foo", false));
//        System.out.println(getValueFromJson(json, "result:info:foo", false));
//        System.out.println(getValueFromJson(json, "result:foo", false));
//        System.out.println(getValueFromJson(json, "foo", false));

//        String regex = "(\\w+)";
//        String input = "i am a student";
//        String result = "";
//        int lastPos = 0;
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(input);
//        while (matcher.find()) {
//            String matchString = matcher.group(1);
//            int pos = matcher.start();
//            result += input.substring(lastPos, pos);
//            result += matchString.toUpperCase();
//            lastPos = matcher.end();
//        }
//        System.out.println(result);
        String baiduURL = "http://waimai.baidu.com/mobile/waimai?qt=shoplist&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&page=1&display=json";
        String shopURL = "http://waimai.baidu.com/mobile/waimai?qt=shopmenu&shop_id=0&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&display=json";
        List<String> shops = new LinkedList<>();
        try {
            String doc = Jsoup.connect(baiduURL).ignoreContentType(true).timeout(20000).execute().body();
            shops.addAll(getValuesFromJson(doc, "shop_id"));
//            int total = Integer.parseInt(getValueFromJson(doc, "total"));
//            System.out.println("total=" + total);
//            int requests = total / 20 + (total % 20 == 0 ? 0 : 1);
//            System.out.println("requests=" + requests);
//            for (int i = 2; i <= requests; ++i) {
//                String regex = "page=\\d+";
//                baiduURL = baiduURL.replaceAll(regex, "page=" + i);
//                doc = Jsoup.connect(baiduURL).ignoreContentType(true).timeout(20000).execute().body();
//                shops.addAll(getValuesFromJson(doc, "shop_name"));
//                Thread.sleep(1000);
//            }
            shopURL=shopURL.replaceAll("shop_id=(\\w+)", "shop_id=" + shops.get(0));
            String shopDoc = Jsoup.connect(shopURL).ignoreContentType(true).timeout(20000).execute().body();
            shops.clear();
            shops.addAll(getValuesFromJson(shopDoc, "data:name", true));
        } catch (Exception e) {
            e.printStackTrace();
        }
        int i = 1;
        for (String shop : shops)
            System.out.println(i++ + ": " + shop);
    }
}
