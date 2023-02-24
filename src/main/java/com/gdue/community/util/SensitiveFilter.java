package com.gdue.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

//过滤敏感词的工具类
@Component
public class SensitiveFilter {

    private static final Logger logger= LoggerFactory.getLogger(SensitiveFilter.class);

    //替换符
    private static final String REPLACEMENT="***";

    private TrieNode   rootNode=new TrieNode();

    //读取敏感词文件
    @PostConstruct
    public void init(){
        try (
                InputStream is=this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader  reader=new BufferedReader(new InputStreamReader(is));

                ) {
            String  keyword;
            while ((keyword=reader.readLine())!=null){
                //添加到前缀树
                this.addKeyword(keyword);
            }

        }catch (IOException e){
            logger.error("加载敏感词文件失败:"+e.getMessage());
        }
    }

    //将一个敏感词添加到前缀树
    private void addKeyword(String  keyword){
        TrieNode  tempNode=rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c=keyword.charAt(i);
            //判断当前前缀树中是否已存在该字符
            TrieNode subNode=tempNode.getSubNode(c);
            if (subNode==null){
                //初始化子节点
                subNode=new TrieNode();
                tempNode.addSubNode(c,subNode);
            }
            //指向子节点,进行下一轮循环
            tempNode=subNode;
            //设置节束标识
            if (i == keyword.length()-1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text  待过滤的文本
     * @return  过滤后的文本
     */
    public String   filter(String   text){
        if (StringUtils.isBlank(text)){
            return null;
        }

        StringBuilder   sb=new StringBuilder();

        //指针1，指向根节点，用来遍历前缀树
        TrieNode    tempNode=rootNode;

        //指针2
        int begin=0;

        //指针3
        int end=0;

        while (begin<text.length()){
            char    c=text.charAt(end);

            //跳过符号
            if (isSymbol(c)){
                //如果指针1处于根节点，跳过该符号，并把它记录下来
                if (tempNode==rootNode){
                    sb.append(c);
                    begin++;
                }
                //无论是不是符号，指针3都走一步
                end++;
                continue;
            }

            //检查下级节点
            tempNode    =   tempNode.getSubNode(c);
            if (tempNode==null){//以begin开头的字符串不是敏感词
                sb.append(c);
                //指针3归位指针2下一位置准备下一次检查
                end=++begin;
                tempNode=rootNode;
            }else if (tempNode.isKeywordEnd()){ //发现敏感词，替换
                sb.append(REPLACEMENT);
                //进入下一位置开始循环检查
                begin=++end;
                tempNode=rootNode;
            }else {
                //检查下一字符
//                end++;
                if (end < text.length() - 1) {
                    end++;
                }
            }
        }
        return sb.toString();
    }

    public String filterPlus(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();

        while(begin < text.length()){
            if(position < text.length()) {
                Character c = text.charAt(position);

                // 跳过符号
                if (isSymbol(c)) {
                    if (tempNode == rootNode) {
                        begin++;
                        sb.append(c);
                    }
                    position++;
                    continue;
                }

                // 检查下级节点
                tempNode = tempNode.getSubNode(c);
                if (tempNode == null) {
                    // 以begin开头的字符串不是敏感词
                    sb.append(text.charAt(begin));
                    // 进入下一个位置
                    position = ++begin;
                    // 重新指向根节点
                    tempNode = rootNode;
                }
                // 发现敏感词
                else if (tempNode.isKeywordEnd()) {
                    sb.append(REPLACEMENT);
                    begin = ++position;
                    tempNode=rootNode;
                }
                // 检查下一个字符
                else {
                    position++;
                }
            }
            // position遍历越界仍未匹配到敏感词
            else{
                sb.append(text.charAt(begin));
                position = ++begin;
                tempNode = rootNode;
            }
        }
        return sb.toString();
    }

    public boolean  isSymbol(Character  c){
        //0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) &&  (c<0x2E80   ||  c>0x9fff);
    }

    //前缀树
    private class TrieNode{

        //关键词节束标识
        private boolean isKeywordEnd    =   false;

        //子节点（key是下级字符，value是下级节点）
        private Map<Character,TrieNode> subNodes=new HashMap<>();

        public boolean  isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean   keywordEnd){
            isKeywordEnd=keywordEnd;
        }

        //添加子节点
        public void addSubNode(Character    c,TrieNode  node){
            subNodes.put(c,node);
        }

        //获取子节点
        public TrieNode getSubNode(Character    c){
            return subNodes.get(c);
        }
    }

}
