<%--
   APDPlat - Application Product Development Platform
   Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
   
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>对栏目下的所有文章计算百度排名API调用演示</title>
    </head>
    <body>
        <h2><font color="blue">对栏目下的所有文章计算百度排名API调用演示</font></h2>
        
        <form action="GetListRank" method="post">
            栏目入口URL地址：<input name="url" size="150" maxlength="150"><br/>
            下一页CSS路径：<input name="nextPageCssQuery" size="150" maxlength="150"><br/>
            下一页标签文本：<input name="nextPageText" size="150" maxlength="150"><br/>
            标题CSS路径：<input name="titleCssQuery" size="150" maxlength="150"><br/>
            <input type="submit" value="获取栏目下所有文章的排名"/>
        </form>
    </body>
</html>