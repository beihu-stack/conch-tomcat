/**
 * Coyote
 * - 封装了底层的网络通信（Socket 请求及响应处理）
 * - 使Catalina 容器（容器组件）与具体的请求协议及IO操作方式完全解耦
 * - 将Socket 输入转换封装为 Request 对象，进一步封装后交由Catalina 容器进行处理，处理请求完成后, Catalina 通过Coyote 提供的Response 对象将结果写入输出流
 * - 负责的是具体协议（应用层）和IO（传输层）相关内容
 * - 一个连接器对应一个监听端口
 *
 * @author Adam
 * @since 2023/12/5
 */
package com.nabob.conch.tomcat.core.coyote;