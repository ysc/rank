/*
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.seo.rank.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 投票排名分析工具
 * @author 杨尚川
 */
public class VoteRanker {

    private VoteRanker(){}

    private static final String WORKS = "html body div.vote-container.block div.container div.vote-project";
    private static final String PROJECT_NAME = "div.project-detail a.project-name";
    private static final String PROJECT_DES = "div.project-detail div.project-description";
    private static final String PROJECT_OWNER = "div.project-detail div.project-owner";
    private static final String VOTE_COUNT = "div.vote-action div.vote-button.vote-trigger span";
    public static Map<String, Integer> getRank(){
        String url = "http://i.100offer.com/projects?page=";
        Map<String, Integer> map = new HashMap<>();
        for(int i=1; i<24; i++) {
            System.out.println("get page "+(url+i));
            try {
                for (Element element : Jsoup.parse(new URL(url + i), 60000).select(WORKS)) {
                    String projectName = element.select(PROJECT_NAME).text();
                    String voteCount = element.select(VOTE_COUNT).text();
                    String des = element.select(PROJECT_DES).text().replace("故事", "");
                    String owner = element.select(PROJECT_OWNER).text().replace("Hot", "").replace("故事", "").replace("by&nbsp", "").replace("by ", "");
                    map.put(projectName+"_"+owner+"_"+des, Integer.parseInt(voteCount));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map;
    }
    public static void main(String[] args){
        Map<String, Integer> data = getRank();
        AtomicInteger i = new AtomicInteger();
        System.out.println("<table>");
        System.out.println("<tr><td>排名</td><td>票数</td><td>项目名称</td><td>项目作者</td><td>项目描述</td></tr>");
        data.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).forEach(e -> {
            String[] value=e.getKey().split("_");
            String projectName = value[0];
            String owner = value[1];
            String des = value[2];
            System.out.println("<tr><td>" + i.incrementAndGet() + "</td><td>" + e.getValue() + "</td><td>" + projectName + "</td><td>" + owner + "</td><td>" + des + "</td></tr>");
        });
        System.out.println("</table>");
    }
}
