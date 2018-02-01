package com.sogn.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;

/**
 * @author songquanhe
 * @note 2017/12/27 12:32
 */
public class HadoopMR extends Mapper<Object, Text, Text, IntWritable> {

    /**
     * 这里我们指定reducer个数为1个，并指定输出格式为.gz。
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * <a href = "http://blog.csdn.net/qiyongkang520/article/details/50988496">参考网址</a>
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: ParseDataToFileElementMR <in> <out>");
            System.exit(2);
        }
        Job job = Job.getInstance(conf, "ParseDataToFileElementMR");
        job.setJarByClass(HadoopMR.class);
        //Mapper
        job.setMapperClass(ParseDataToFileElementMapper.class);

        //Combiner
//        job.setCombinerClass(ParseDataToFileElementReducer.class);

        //Reducer
        job.setReducerClass(ParseDataToFileElementReducer.class);
        job.setNumReduceTasks(10);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));

        //将reduce输出文件压缩.gz
        FileOutputFormat.setCompressOutput(job, true);  //job使用压缩
        FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class); //设置压缩格式

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    /**
     * map过程
     */
    public static class ParseDataToFileElementMapper extends Mapper<Object, Text, Text, IntWritable> {

        private static final IntWritable one = new IntWritable(1);
        private Text mapKey = new Text();

        @Override
        protected void map(Object key, Text value, Mapper<Object, Text, Text, IntWritable>.Context context)
                throws IOException, InterruptedException {
            String[] values = value.toString().split("\\|");

            if ("r".equals(values[10])) {

                mapKey.set(values[5] + "\t" + values[0] + "\t" + values[2]);
                System.out.println(mapKey.toString());
                context.write(mapKey, one);
            }
        }
    }

    /**
     * reduce过程
     */
    public static class ParseDataToFileElementReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private Text reduceKey = new Text();
        private IntWritable result = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Reducer<Text, IntWritable, Text, IntWritable>.Context context)
                throws IOException, InterruptedException {
            //把key相同的统计一下次数
            //cname + topDomain + cip + dip
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            this.result.set(sum);
            this.reduceKey.set("1.1-1.1" + "\t" + key.toString());

            context.write(this.reduceKey, this.result);
        }

    }



}
