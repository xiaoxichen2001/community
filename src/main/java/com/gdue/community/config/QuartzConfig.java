package com.gdue.community.config;

import com.gdue.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

//初次配置初始化，存在数据库中，之后访问数据库查询配置
@Configuration
public class QuartzConfig {

    /**
     * FactoryBean可以简化Bean的实例过程
     * 1.通过FactoryBean封装Bean的实例化过程
     * 2.将FactoryBean装配到spring容器
     * 3.将FactoryBean注入到其他的Bean
     * 4.其他的Bean得到的时Factory所管理的对象实例
     */

    //刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail(){
        JobDetailFactoryBean    factoryBean =new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    //配置Trigger(SimpleTriggerFactoryBean, CronTriggerFactoryBean)
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail   postScoreRefreshJobDetail){
        SimpleTriggerFactoryBean    factoryBean=new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000*60*5);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }
}
