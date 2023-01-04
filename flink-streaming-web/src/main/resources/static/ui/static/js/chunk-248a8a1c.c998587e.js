(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-248a8a1c"],{"33f0":function(e,t,a){},"3db4":function(e,t,a){"use strict";a.r(t);var n=function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("div",{directives:[{name:"loading",rawName:"v-loading",value:e.loading,expression:"loading"}],staticClass:"fl-container"},[e._m(0),e._m(1),a("el-form",{ref:"queryform",attrs:{model:e.queryform,inline:!0}},[a("el-form-item",[a("el-form-item",[a("el-input",{staticClass:"wl-input",attrs:{placeholder:"文件名(模糊查询)"},on:{input:function(t){return e.handleQuery()}},model:{value:e.queryform.fileName,callback:function(t){e.$set(e.queryform,"fileName",t)},expression:"queryform.fileName"}},[a("el-button",{staticClass:"wl-search",attrs:{slot:"append",type:"primary",icon:"el-icon-search"},on:{click:function(t){return e.handleQuery()}},slot:"append"})],1)],1),a("uploader",{ref:"uploader",staticClass:"uploader-example",attrs:{options:e.options,"file-status-text":e.fileStatusText},on:{"file-complete":e.fileComplete,complete:e.complete}},[a("uploader-unsupport"),a("uploader-drop",[a("p",[e._v("请上传jar包")]),a("uploader-btn",{attrs:{attrs:e.attrs}},[e._v("上传jar文件")]),a("uploader-btn",{attrs:{directory:!0}},[e._v("选择文件夹")])],1),a("uploader-list")],1)],1)],1),a("el-table",{staticClass:"wl-table",attrs:{data:e.list,"header-cell-style":{background:"#f4f4f5","text-align":"center"},border:""}},[a("el-table-column",{attrs:{prop:"id","show-overflow-tooltip":!0,label:"编号","min-width":"50",width:"80",align:"center"}}),a("el-table-column",{attrs:{prop:"fileName","show-overflow-tooltip":!0,label:"文件名",align:"center"}}),a("el-table-column",{attrs:{prop:"downloadJarHttp","show-overflow-tooltip":!0,label:"http地址","min-width":"40",align:"center"}}),a("el-table-column",{attrs:{prop:"createTimeStr","show-overflow-tooltip":!0,label:"上传时间","min-width":"25",align:"center"}}),a("el-table-column",{attrs:{prop:"operate",label:"操作",width:"180",fixed:"right",align:"center"},scopedSlots:e._u([{key:"default",fn:function(t){return[a("el-link",{attrs:{type:"primary",icon:"el-icon-delete"},nativeOn:{click:function(a){return e.deleteFile(t.row)}}},[e._v("删除")]),a("el-link",{attrs:{type:"primary"},nativeOn:{click:function(a){return e.doCopy(t.row)}}},[e._v("复制URL")]),a("el-link",{attrs:{type:"primary"},nativeOn:{click:function(a){return e.doCopyFileName(t.row)}}},[e._v("复制文件名")])]}}])})],1),e.pageshow?a("el-pagination",{staticClass:"wl-pagination",attrs:{background:"",layout:"total, sizes, prev, pager, next","current-page":e.currentPage,"page-sizes":[10,15,20,50,100,150,200],"page-size":e.pageSize,total:e.count},on:{"size-change":e.handleSizeChange,"current-change":e.handleCurrentChange}}):e._e()],1)},r=[function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("div",{staticStyle:{"margin-bottom":"2px",color:"red"}},[a("span",{staticClass:"wl-title"},[e._v("1、不支持集群部署的时候使用此功能,另外jar全部存在本地服务器上 ./flink-streaming-platform-web/upload_jars")])])},function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("div",{staticStyle:{"margin-bottom":"2px",color:"red"}},[a("span",{staticClass:"wl-title"},[e._v("2、该功能主要是提供了jar管理:如：连接器jar、udf的jar 等 方便创建SQL流任务需要的jar")])])}],o=(a("498a"),a("b775")),i=a("4328"),l=a.n(i);function s(e,t,a){return Object(o["a"])({url:"/queryUploadFile",method:"post",headers:{"Content-Type":"application/x-www-form-urlencoded"},transformRequest:[function(e){return l.a.stringify(e)}],data:{pageNum:e,pageSize:t,fileName:a}})}function c(e){return Object(o["a"])({url:"/deleteFile",method:"post",headers:{"Content-Type":"application/x-www-form-urlencoded"},transformRequest:[function(e){return l.a.stringify(e)}],data:{id:e}})}var u={name:"UploadManage",data:function(){return{options:{target:"/api/upload",chunkSize:1073741824,testChunks:!1},attrs:{accept:[".JAR"]},fileStatusText:function(e,t){var a={success:"成功了",error:"出错了",uploading:"上传中",paused:"暂停中",waiting:"等待中"};return"success"===e||"error"===e?200==t.code?a[e]:(alert(t.message),a["error"]):a[e]},loading:!0,queryform:{fileName:""},list:[],count:0,pageSize:10,currentPage:1,pageshow:!0}},mounted:function(){var e=this;this.handleQuery(),this.$nextTick((function(){window.uploader=e.$refs.uploader.uploader}))},methods:{handleQuery:function(e){var t=this;this.pageshow=!1,this.queryUserList(),this.$nextTick((function(){t.pageshow=!0}))},handleSizeChange:function(e){this.pageSize=e,this.handleQuery()},complete:function(){console.log("complete",arguments),this.queryUserList()},fileComplete:function(){this.queryUserList(),console.log("file complete",arguments)},handleCurrentChange:function(e){this.currentPage=e,this.handleQuery()},queryUserList:function(){var e=this;this.loading=!0;var t=this.queryform.fileName?this.queryform.fileName.trim():"";s(this.currentPage,this.pageSize,t).then((function(t){e.loading=!1;var a=t.code,n=t.success,r=t.message,o=t.data;"200"===a&&n?(e.count=o.total,e.list=o.data,e.count>0&&0===e.list.length&&(e.currentPage=Math.ceil(e.count/e.pageSize),e.queryUserList())):e.$message({type:"error",message:r||"请求数据异常！"})})).catch((function(t){e.loading=!1,e.$message({type:"error",message:"请求异常！"}),console.log(t)}))},doCopy:function(e){e.id,e.fileName;var t=e.downloadJarHttp;this.$copyText(t).then((function(e){alert("复制jar地址成功:"+t)}),(function(e){alert("Can not copy"),console.log(e)}))},doCopyFileName:function(e){e.id;var t=e.fileName;e.downloadJarHttp;this.$copyText(t).then((function(e){alert("复制jar文件名字成功:"+t)}),(function(e){alert("Can not copy"),console.log(e)}))},deleteFile:function(e){var t=this,a=e.id,n=e.fileName;this.$confirm("确定要删除[".concat(n,"]吗？"),"提示",{confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"}).then((function(){t.loading=!0,c(a).then((function(e){t.loading=!1;var a=e.code,r=e.success,o=e.message;e.data;"200"===a&&r?(t.handleQuery(),t.$message({type:"success",message:"删除[".concat(n,"]成功！")})):t.$message({type:"error",message:o||"请求数据异常！"})})).catch((function(e){t.loading=!1,t.$message({type:"error",message:"请求异常！"}),console.log(e)}))}))}}},p=u,d=(a("bff6"),a("2877")),f=Object(d["a"])(p,n,r,!1,null,"255399f0",null);t["default"]=f.exports},bff6:function(e,t,a){"use strict";a("33f0")}}]);