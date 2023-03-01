package com.gdue.community;

import com.alibaba.fastjson.JSONObject;
import com.gdue.community.dao.UserMapper;
import com.gdue.community.entity.User;
import com.gdue.community.util.MailClient;
import com.gdue.community.dao.DiscussPostMapper;
import com.gdue.community.dao.LoginTicketMapper;
import com.gdue.community.elasticsearch.DiscussPostRepository;
import com.gdue.community.entity.DiscussPost;
import com.gdue.community.entity.LoginTicket;
import com.gdue.community.util.SensitiveFilter;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CommunityApplication.class)
class CommunityApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginTicketMapper   loginTicketMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostMapper   discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

//    @Test
//    public void testSelectById(){
//        User user = userDao.selectById(0);
//        System.out.println(user);
//    }

    @Autowired
    private MailClient mailClient;

    @Test
    public void testTextMail(){
        mailClient.sendMail("xiaoxichen2001@163.com","Test","success");
    }

    @Test
    public void testUserMapper(){
        User user = userMapper.selectByName("zhangsan");
//        User user = userMapper.selectById(1);
        System.out.println(user);
    }

    @Test
    public void testLoginTicletMapper(){
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setTicket("1wwww");
        loginTicket.setExpired(new Date());
        loginTicket.setStatus(1);
        loginTicket.setUserId(1111);
        loginTicketMapper.insertLoginTicket(loginTicket);

        LoginTicket loginTicket1 = loginTicketMapper.selectByTicket("1wwww");
        System.out.println(loginTicket1);
    }

    @Test
    public void testSensitiveFilter(){
        String  text    =   "我爱赌博吸烟喝酒嫖娼，少年当自强，就该@吃喝@嫖@赌";
        String filter = sensitiveFilter.filter(text);
        System.out.println(filter);

        System.out.println("===============");
        String filterPlus = sensitiveFilter.filterPlus(text);
        System.out.println(filterPlus);
    }




        //判断某id的文档（数据库中的行）是否存在
        @Test
        public void testExist(){
            boolean exists =discussPostRepository.existsById(241);
            System.out.println(exists);
        }

        //一次保存一条数据
        @Test
        public void testInsert() {
            //把id为241的DiscussPost的对象保存到discusspost索引（es的索引相当于数据库的表）
            DiscussPost post = discussPostMapper.selectDiscussPostById(241);
            System.out.println(post);
            discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        }

        //一次保存多条数据
        @Test
        public void testInsertList() {
            //把id为101的用户发的前100条帖子（List<DiscussPost>）存入es的discusspost索引（es的索引相当于数据库的表）
            discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(101, 0, 100,0));
        }

        //通过覆盖原内容，来修改一条数据
        @Test
        public void testUpdate() {
            DiscussPost post = discussPostMapper.selectDiscussPostById(230);
            post.setContent("我是新人,使劲灌水。");
            post.setTitle(null);//es中的title会设为null
            discussPostRepository.save(post);
        }

//        //修改一条数据
//        //覆盖es里的原内容 与 修改es中的内容 的区别：String类型的title被设为null，覆盖的话，会把es里的该对象的title也设为null；UpdateRequest，修改后该对象的title不变
//        @Test
//        void testUpdateDocument() throws IOException{
//            UpdateRequest request = new UpdateRequest("discusspost", "109");
//            request.timeout("1s");
//            DiscussPost post = discussMapper.selectDiscussPostById(230);
//            post.setContent("我是新人,使劲灌水.");
//            post.setTitle(null);//es中的title会保存原内容不变
//            request.doc(JSON.toJSONString(post), XContentType.JSON);
//            UpdateResponse updateResponse = restHighLevelClient.update(request, RequestOptions.DEFAULT);
//            System.out.println(updateResponse.status());
//        }

        //删除一条数据和删除所有数据
        @Test
        public void testDelete() {
            discussPostRepository.deleteById(109);//删除一条数据
            //discussRepository.deleteAll();//删除所有数据
        }

        //不带高亮的查询
        @Test
        public void noHighlightQuery() throws IOException {
            SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名

            //构建搜索条件
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    //在discusspost索引的title和content字段中都查询“互联网寒冬”
                    .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                    // matchQuery是模糊查询，会对key进行分词：searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
                    // termQuery是精准查询：searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
                    .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                    .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                    .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                    //一个可选项，用于控制允许搜索的时间：searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
                    .from(0)// 指定从哪条开始查询
                    .size(10);// 需要查出的总记录条数

            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = null;
            try {
                searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(JSONObject.toJSON(searchResponse));

            List<DiscussPost> list = new LinkedList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
                System.out.println(discussPost);
                list.add(discussPost);
            }
        }

        //带高亮的查询
        @Test
        public void highlightQuery() throws Exception{
            SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名

            //高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.field("content");
            highlightBuilder.requireFieldMatch(false);
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");

            //构建搜索条件
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                    .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                    .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                    .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                    .from(0)// 指定从哪条开始查询
                    .size(10)// 需要查出的总记录条数
                    .highlighter(highlightBuilder);//高亮

            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            List<DiscussPost> list = new LinkedList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);

                // 处理高亮显示的结果
                HighlightField titleField = hit.getHighlightFields().get("title");
                if (titleField != null) {
                    discussPost.setTitle(titleField.getFragments()[0].toString());
                }
                HighlightField contentField = hit.getHighlightFields().get("content");
                if (contentField != null) {
                    discussPost.setContent(contentField.getFragments()[0].toString());
                }
                System.out.println(discussPost);
                list.add(discussPost);
            }
        }
}
