项目概述
====
本项目功能是将某台机器的某个端口的流量回放到指定的机器上,支持同步\异步方式


gor工具原理
-------

基于github上的一个Go语言写的开源流量复制工具gor的扩展
* gor项目地址:https://github.com/buger/gor
* 到 https://github.com/buger/gor/releases 下载最新版本
    分为mac和linux,按自己平台选择,或者选择本项目里的两个tar包
* 解压后是一个名叫gor的文件,使用方式如下
1. 在本机8080端口部署一个web应用
2. 在另外一台机器比如192.168.11.22的8080端口部署同样的应用
3. 在本机有sudo权限的用户下执行如下命令
sudo ./gor --input-raw :8080 --output-http http://192.168.11.22:8080
即可将当前机器的8080端口流量同步回放到http://192.168.11.22:8080端口
所以访问本机8080的应用就可以看到同时在http://192.168.11.22:8080也有请求过去了
* gor工具所有的命令参数说明可以看执行sudo ./gor -h 命令查看


基于gor工具的扩展
----------
添加了一个java的class文件,go.middleware.Stdout,该功能支持在配置文件中用添加url表达式,用于只将指定的URL复制出来
* 源码见src目录下
* 执行的命令是
./gor --input-raw-track-response --input-raw :8082 --middleware "java go.middleware.Stdout" --output-file 
即加一个参数: --middleware "java go.middleware.Stdout"
* 注意,要将该文件路径放置到与gor文件同级目录下的gor/middleware/下面
* 生产上执行的命令如下
nohup ./gor --input-raw-track-response --input-raw :8082 --middleware "java go.middleware.Stdout" --output-file gor-online-%Y-%m-%d-%H.log --output-file-append  >dev/null &
将请求保存到文件中用于异步回放


文件结果比对
------
CompareHttpLog文件用于将两个文件结果进行比对,支持json格式结果中只比较部分json节点

