import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sider {
    static Pattern rex = Pattern.compile(".*\\?pageIndex=2&pageCount=(\\d*)");
    static Pattern idRex = Pattern.compile("http://www.baikemy.com/disease/view/(\\d*)");

    public static void main(String[] args) {
//        getKeshi();
//        getURLList("http://www.baikemy.com/disease/list/1/33");
//        getJibingUrl("http://www.baikemy.com/disease/list/24/0","2");
        dealAllUrl();

    }


    private static void dealAllUrl() {

        List<String> urlList = null;
        try {
            urlList = FileUtils.readLines(new File("所有疾病.txt"), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<HashMap<String, Object>> list = new LinkedList<>();
        for (int i = 0; i < urlList.size(); i++) {
            System.out.println(i + "\t" + urlList.size() + "\t" + Float.valueOf(i) / Float.valueOf(urlList.size()));
            try {
                String[] words = urlList.get(i).split("\t");
                String url = words[1];
                String keshi = words[2];
                String name = words[0];

                HashMap<String, Object> one = new HashMap<>();
                Matcher matcher = idRex.matcher(url);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    String newUrl = "http://www.baikemy.com/disease/detail/" + id + "/1";

                    Document d = Jsoup.connect(newUrl).get();
                    String type = d.selectFirst("div.jb-mb > a:nth-child(5)").text();
                    Elements es = d.select(".lemma-main-content");
                    List<HashMap<String, String>> info = new ArrayList<>();
                    try {
                        for (Element e : es) {
                            String index = e.selectFirst(".headline-1-index").text();
                            String title = e.selectFirst(".headline-content").text();
                            StringBuilder para = new StringBuilder();
                            for (Element s : e.select(".para p")) {
                                para.append(s.text() + "\n");
                            }
                            HashMap<String, String> map = new HashMap<>();
                            map.put("index", index);
                            map.put("title", title);
                            map.put("para", para.toString());
                            info.add(map);
                        }
                    } catch (Exception e2) {
                        HashMap<String, String> map = new HashMap<>();
                        String para = d.selectFirst(".lemma-main-content").text();
                        map.put("index", "一");
                        map.put("title", "介绍");
                        map.put("para", para);
                    }
                    one.put("id", id);
                    one.put("url", newUrl);
                    one.put("name", name);
                    one.put("keshi", keshi);
                    one.put("type", type);
                    one.put("info", info);
                } else {
                    FileUtils.write(new File("wentiURL_lite.txt"), url + "\n", "utf-8", true);
                }
                list.add(one);
            } catch (Exception e) {
                System.out.println();
                try {
                    FileUtils.write(new File("wentiURL.txt"), urlList.get(i) + "\n", "utf-8", true);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        try {
            FileUtils.write(new File("所有结果.json"), toJsonBeautiful(list), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        FileUtils.write(new File("所有结果.json"), JSON.toJSONString(list,true), "utf-8");

    }

    private static void getKeshi() throws IOException {
        Document d = Jsoup.connect("http://www.baikemy.com/disease/list/1/34").get();
        for (Element element : d.select(".yiyao_w870 ul li")) {
            Element e = element.selectFirst("a");
            String keshi = e.text();
            String url = e.absUrl("href");
            FileUtils.write(new File("所有科室.txt"), keshi + "\t" + url + "\n", "utf-8", true);
            List<String> urlList = getURLList(url);
            for (String s : urlList) {
                getJibingUrl(s, keshi);
            }
            System.out.println(keshi + "->" + urlList.size());

        }
    }

    private static List<String> getURLList(String url) throws IOException {
        List<String> urlList = new ArrayList<>();
        urlList.add(url);
        Document root = Jsoup.connect(url).get();
        Elements pageElements = root.select("div.bigPage.clearFix a");
        if (pageElements.size() > 0) {
            String href = pageElements.first().absUrl("href");
            Matcher matcher = rex.matcher(href);
            if (matcher.find()) {
                int pageSum = Integer.valueOf(matcher.group(1));
                for (int i = 2; i <= pageSum; i++) {
                    urlList.add(url + "?pageIndex=" + i + "&pageCount=" + pageSum);
                }
            }
        }
        return urlList;
    }

    private static void getJibingUrl(String url, String keshi) throws IOException {
        Document d = Jsoup.connect(url).get();
        Elements dElements = d.select(" div.ccyy_jbl > div > div > ul >li");
        for (Element element : dElements) {
            String name = element.text();
            String dUrl = element.selectFirst("a").absUrl("href");
            System.out.println(dUrl);
            FileUtils.write(new File("所有疾病.txt"), name + "\t" + dUrl + "\t" + keshi + "\n", "utf-8", true);
        }
    }

    private static String toJsonBeautiful(Object o) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(o);
    }
}
