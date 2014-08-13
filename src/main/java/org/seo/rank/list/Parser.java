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

package org.seo.rank.list;

import java.util.List;
import org.seo.rank.model.Article;

/**
 * 解析所有的列表页面
 * 获取文章的标题和URL
 * @author 杨尚川
 */
public interface Parser {
    /**
     * 解析列表页面
     * @param url 列表页面第一页
     * @param nextPageCssQuery 获取下一页的CSS路径
     * @param nextPageText 获取下一页的CSS路径元素中的文本值
     * @param titleCssQuery 提取文章标题的CSS路径
     * @return 
     */
    public List<Article> parse(String url, String nextPageCssQuery, String nextPageText, String titleCssQuery);
}
