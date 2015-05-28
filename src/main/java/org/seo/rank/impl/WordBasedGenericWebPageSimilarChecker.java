/**
 *
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.seo.rank.impl;

import org.apdplat.word.analysis.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.seo.rank.SimilarChecker;
import org.seo.rank.list.impl.DefaultParser;
import org.seo.rank.model.Article;
import org.seo.rank.tools.DynamicIp;
import org.seo.rank.tools.ProxyIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 基于word分词提供的文本相似度算法来实现通用的网页相似度检测
 * @author 杨尚川
 */
public class WordBasedGenericWebPageSimilarChecker implements SimilarChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordBasedGenericWebPageSimilarChecker.class);
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:36.0) Gecko/20100101 Firefox/36.0";
    private static final float THRESHOLD_RATE = 0.5F;
    private TextSimilarity textSimilarity = new EditDistanceTextSimilarity();

    public WordBasedGenericWebPageSimilarChecker(){}

    public WordBasedGenericWebPageSimilarChecker(TextSimilarity textSimilarity){
        this.textSimilarity = textSimilarity;
    }

    public void setTextSimilarity(TextSimilarity textSimilarity) {
        this.textSimilarity = textSimilarity;
    }

    @Override
    public boolean isSimilar(String url1, String url2) {
        return similarScore(url1, url2)>=THRESHOLD_RATE;
    }

    @Override
    public double similarScore(String url1, String url2) {
        WebPage webPage1 = getWebPage(url1);
        if(webPage1!=null) {
            WebPage webPage2 = getWebPage(url2);
            if(webPage2!=null) {
                double score = textSimilarity.similarScore(webPage1.getContent(), webPage2.getContent());
                return score;
            }
        }
        return 0.0;
    }

    public String contrastSimilarScore(String url1, String url2, List<TextSimilarity> textSimilarities) {
        StringBuilder result = new StringBuilder();
        WebPage webPage1 = getWebPage(url1);
        if(webPage1!=null) {
            WebPage webPage2 = getWebPage(url2);
            if(webPage2!=null) {
                textSimilarities.forEach(textSimilarity -> {
                    double score = textSimilarity.similarScore(webPage1.getContent(), webPage2.getContent());
                    result.append(textSimilarity.getClass().getSimpleName().replace("TextSimilarity", ""))
                            .append("=")
                            .append(score)
                            .append(" ");
                });
            }
        }
        return result.toString();
    }

    private WebPage getWebPage(String url){
        WebPage webPage = getWebPageInternal(url);
        int times = 1;
        while (webPage==null && times<4){
            times++;
            //使用新的IP地址
            DynamicIp.toNewIp();
            webPage = getWebPageInternal(url);
        }
        if(webPage==null){
            return null;
        }
        times = 1;
        //LOGGER.debug("获取到的HTML：" +html);
        while((webPage.getContent().contains("非常抱歉，来自您ip的请求异常频繁")
                || webPage.getContent().contains("请您点击按钮解除封锁")
                || webPage.getContent().contains("请输入以下验证码"))
                && times<4){
            times++;
            //使用新的IP地址
            ProxyIp.toNewIp();
            webPage = getWebPageInternal(url);
        }
        return webPage;
    }
    private WebPage getWebPageInternal(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Referer", "http://"+new URL(url).getHost())
                    .header("Host", new URL(url).getHost())
                    .header("User-Agent", USER_AGENT)
                    .header("X-Forwarded-For", getRandomIp())
                    .header("Proxy-Client-IP", getRandomIp())
                    .header("WL-Proxy-Client-IP", getRandomIp())
                    .ignoreContentType(true)
                    .timeout(30000)
                    .get();
            WebPage webPage = new WebPage();
            webPage.setUrl(url);
            webPage.setContent(doc.text());
            webPage.setTitle(doc.title());
            return webPage;
        } catch (Exception e) {
            LOGGER.error("获取网页失败", e);
        }
        return null;
    }
    private String getRandomIp(){
        int first = new Random().nextInt(254)+1;
        //排除A类私有地址0.0.0.0--10.255.255.255
        while(first==10){
            first = new Random().nextInt(254)+1;
        }
        int second = new Random().nextInt(254)+1;
        //排除B类私有地址172.16.0.0--172.31.255.255
        while(first==172 && (second>=16 && second<=31)){
            first = new Random().nextInt(254)+1;
            second = new Random().nextInt(254)+1;
        }
        //排除C类私有地址192.168.0.0--192.168.255.255
        while(first==192 && second==168){
            first = new Random().nextInt(254)+1;
            second = new Random().nextInt(254)+1;
        }
        int third = new Random().nextInt(254)+1;
        int forth = new Random().nextInt(254)+1;
        return first+"."+second+"."+second+"."+forth;
    }
    private static class WebPage{
        private String url;
        private String title;
        private String content;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 我的ITEYE和OSCHINA博客有很多同样的博文，主要目的是备份
     * 这里刚好用来测试相似性检测算法的效果
     * http://yangshangchuan.iteye.com
     * http://my.oschina.net/apdplat/blog
     */
    private void verifyYscBlog(List<TextSimilarity> textSimilarities){
        List<Article> ob = DefaultParser.oschinaBlog();
        List<Article> ib = DefaultParser.iteyeBlog();
        Map<String, String> om = new HashMap<>();
        Map<String, String> im = new HashMap<>();
        ob.stream().forEach(b->om.put(b.getTitle(), b.getUrl()));
        ib.stream().forEach(b->im.put(b.getTitle(), b.getUrl()));
        List<String> oschinaBlog = ob.stream().map(b -> b.getTitle()).sorted().collect(Collectors.toList());
        List<String> iteyeBlog   = ib.stream().map(b -> b.getTitle()).sorted().collect(Collectors.toList());

        List<String> commons = oschinaBlog.stream().filter(b -> iteyeBlog.contains(b)).collect(Collectors.toList());
        commons.remove("自动更改IP地址反爬虫封锁，支持多线程");
        Map<String, String> result = new HashMap<>();
        AtomicInteger similarCount = new AtomicInteger();
        AtomicInteger j = new AtomicInteger();
        commons.forEach(title -> {
            String contrastResult = contrastSimilarScore(om.get(title), im.get(title), textSimilarities);
            LOGGER.info(contrastResult+" "+title+" "+om.get(title)+" "+im.get(title));
            result.put(title, contrastResult);
            LOGGER.info("进度：" + commons.size() + "/" + j.incrementAndGet());
        });
        LOGGER.info("<h4>检查的博文数：" + commons.size() + "</h4>");
        AtomicInteger i = new AtomicInteger();
        result
            .entrySet()
            .stream()
            .forEach(e -> {
                LOGGER.info("");
                LOGGER.info("<h4>"+i.incrementAndGet() + "、检查博文" + "：" + e.getKey()+"，相似度分值："+e.getValue()+"</h4>");
                LOGGER.info("\t博文地址1：<a target=\"_blank\" href=\""+om.get(e.getKey())+"\">"+om.get(e.getKey())+"</a><br/>");
                LOGGER.info("\t博文地址2：<a target=\"_blank\" href=\""+im.get(e.getKey())+"\">"+im.get(e.getKey())+"</a><br/>");
            });
    }
    public static void main(String[] args) throws Exception{
        List<TextSimilarity> textSimilarities = Arrays.asList(new SimpleTextSimilarity(),
                new CosineTextSimilarity(),
                new EditDistanceTextSimilarity(),
                new EuclideanDistanceTextSimilarity(),
                new ManhattanDistanceTextSimilarity(),
                new JaccardTextSimilarity(),
                new JaroDistanceTextSimilarity(),
                new JaroWinklerDistanceTextSimilarity(),
                new SørensenDiceCoefficientTextSimilarity(),
                new SimHashPlusHammingDistanceTextSimilarity());
        WordBasedGenericWebPageSimilarChecker similarChecker = new WordBasedGenericWebPageSimilarChecker();
        similarChecker.verifyYscBlog(textSimilarities);
    }
}
