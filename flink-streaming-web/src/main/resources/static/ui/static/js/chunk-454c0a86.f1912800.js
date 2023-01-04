(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-454c0a86"],{"3abe":function(e,t,a){},7169:function(e,t,a){"use strict";a("3abe")},"8c0e":function(e,t,a){"use strict";a.r(t);var o=function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("div",{directives:[{name:"loading",rawName:"v-loading",value:e.loading,expression:"loading"}],staticClass:"fl-container"},[a("el-form",{ref:"queryform",attrs:{model:e.queryform,inline:!0}},[a("el-form-item",[a("el-input",{staticClass:"wl-input",attrs:{placeholder:"任务Id"},on:{input:function(t){return e.handleQuery()}},model:{value:e.queryform.jobConfigId,callback:function(t){e.$set(e.queryform,"jobConfigId",t)},expression:"queryform.jobConfigId"}})],1),a("el-form-item",[a("el-select",{attrs:{placeholder:"选择状态",clearable:""},on:{change:function(t){return e.handleQuery()}},model:{value:e.queryform.status,callback:function(t){e.$set(e.queryform,"status",t)},expression:"queryform.status"}},[a("el-option",{attrs:{label:"成功",value:"1"}}),a("el-option",{attrs:{label:"失败",value:"0"}})],1)],1),a("el-form-item",[a("el-button",{attrs:{type:"primary"},on:{click:function(t){return e.handleQuery()}}},[e._v("查询")])],1)],1),a("el-table",{staticClass:"wl-table",attrs:{data:e.list,"header-cell-style":{background:"#f4f4f5","text-align":"center"},border:""}},[a("el-table-column",{attrs:{prop:"id","show-overflow-tooltip":!0,label:"编号","min-width":"60",width:"80",align:"center",fixed:""}}),a("el-table-column",{attrs:{"show-overflow-tooltip":!0,label:"任务ID","min-width":"60",width:"80",align:"center",fixed:""},scopedSlots:e._u([{key:"default",fn:function(t){return[a("span",{staticStyle:{"margin-right":"5px"}},[e._v(e._s(t.row.jobConfigId))]),a("el-tooltip",{staticClass:"item",attrs:{effect:"dark",content:e.getTaskTypeName(t.row.jobTypeEnum),placement:"right"}},["SQL_STREAMING"===t.row.jobTypeEnum?a("i",{staticClass:"iconfont my-icon-jiediansql",staticStyle:{"font-size":"16px"}}):e._e(),"SQL_BATCH"===t.row.jobTypeEnum?a("i",{staticClass:"iconfont my-icon-file-SQL",staticStyle:{"font-size":"16px"}}):e._e(),"JAR"===t.row.jobTypeEnum?a("i",{staticClass:"iconfont my-icon-suffix-jar",staticStyle:{"font-size":"16px"}}):e._e()])]}}])}),a("el-table-column",{attrs:{prop:"jobName","show-overflow-tooltip":!0,label:"任务名称","min-width":"80",width:"150",align:"center",fixed:""}}),a("el-table-column",{attrs:{prop:"alarMLogTypeEnum","show-overflow-tooltip":!0,label:"告警类型",width:"110",align:"center"},scopedSlots:e._u([{key:"default",fn:function(t){return[a("span",[e._v(e._s(e.getAlarMLogType(t.row.alarMLogTypeEnum)))])]}}])}),a("el-table-column",{attrs:{prop:"message","show-overflow-tooltip":!0,label:"消息内容","min-width":"200",align:"center"}}),a("el-table-column",{attrs:{prop:"alarmLogStatusEnum",label:"状态",width:"65",align:"center"},scopedSlots:e._u([{key:"default",fn:function(t){return["FAIL"===t.row.alarmLogStatusEnum?a("el-tag",{attrs:{type:"danger",size:"mini"}},[e._v(e._s(e.getAlarmLogStatus(t.row.alarmLogStatusEnum)))]):"SUCCESS"===t.row.alarmLogStatusEnum?a("el-tag",{attrs:{type:"success",size:"mini"}},[e._v(e._s(e.getAlarmLogStatus(t.row.alarmLogStatusEnum)))]):a("el-tag",{attrs:{type:"info",size:"mini"}},[e._v(e._s(e.getStatusDesc(t.row.alarmLogStatusEnum)))])]}}])}),a("el-table-column",{attrs:{prop:"createTime","show-overflow-tooltip":!0,label:"告警时间","min-width":"100",width:"135",align:"center"},scopedSlots:e._u([{key:"default",fn:function(t){return[a("span",[e._v(e._s(e.formatDateTime(t.row.editTime)))])]}}])}),a("el-table-column",{attrs:{prop:"operate",label:"操作",width:"100",align:"center"},scopedSlots:e._u([{key:"default",fn:function(t){return[a("el-link",{attrs:{icon:"el-icon-message"},on:{click:function(a){return e.getLogErrorInfo(t.row.id)}}},[e._v("附加日志")])]}}])})],1),e.pageshow?a("el-pagination",{staticClass:"wl-pagination",attrs:{background:"",layout:"total, sizes, prev, pager, next","current-page":e.currentPage,"page-sizes":[10,15,20,50,100,150,200],"page-size":e.pageSize,total:e.count},on:{"size-change":e.handleSizeChange,"current-change":e.handleCurrentChange}}):e._e(),a("el-dialog",{attrs:{title:"错误信息",visible:e.dialogFailLogVisible},on:{"update:visible":function(t){e.dialogFailLogVisible=t}}},[a("p",[e._v(e._s(e.failLog))])])],1)},n=[],r=a("b775"),i=a("4328"),s=a.n(i);function l(e,t,a,o){return Object(r["a"])({url:"/alartLogList",method:"post",headers:{"Content-Type":"application/x-www-form-urlencoded"},transformRequest:[function(e){return s.a.stringify(e)}],data:{pageNum:e,pageSize:t,jobConfigId:a,status:o}})}function u(e){return Object(r["a"])({url:"/logErrorInfo",method:"post",headers:{"Content-Type":"application/x-www-form-urlencoded"},transformRequest:[function(e){return s.a.stringify(e)}],data:{id:e}})}var c={name:"AlarmLogs",data:function(){return{loading:!1,dialogFailLogVisible:!1,failLog:"",queryform:{jobConfigId:"",status:""},list:[],count:0,pageSize:15,currentPage:1,pageshow:!0}},mounted:function(){var e=this.$route.params;e&&(this.count=e.count,this.currentPage=e.currentPage,this.pageSize=e.pageSize,this.queryform.jobConfigId=e.jobConfigId,this.queryform.status=e.status),this.handleQuery()},methods:{handleQuery:function(e){var t=this;this.pageshow=!1,this.getLogs(),this.$nextTick((function(){t.pageshow=!0}))},handleSizeChange:function(e){this.pageSize=e,this.handleQuery()},handleCurrentChange:function(e){this.currentPage=e,this.handleQuery()},getLogs:function(){var e=this;this.loading=!0;var t=this.queryform,a=t.jobConfigId,o=t.status;l(this.currentPage,this.pageSize,a,o).then((function(t){e.loading=!1;var a=t.code,o=t.success,n=t.message,r=t.data;"200"===a&&o?(e.count=r.total,e.list=r.data,e.count>0&&0==e.list.length&&(e.currentPage=Math.ceil(e.count/e.pageSize),e.getLogs())):e.$message({type:"error",message:n||"请求数据异常！"})})).catch((function(t){e.loading=!1,e.$message({type:"error",message:"请求异常！"}),console.log(t)}))},getLogErrorInfo:function(e){var t=this;this.loading=!0,this.failLog="",u(e).then((function(e){t.loading=!1;var a=e.code,o=e.success,n=e.message,r=e.data;"200"===a&&o?(t.failLog=r,t.dialogFailLogVisible=!0):t.$message({type:"error",message:n||"请求数据异常！"})})).catch((function(e){t.loading=!1,t.$message({type:"error",message:"请求异常！"}),console.log(e)}))},getTaskTypeName:function(e){switch(e){case"SQL_STREAMING":return"SQL流任务";case"SQL_BATCH":return"SQL批任务";case"JAR":return"JAR包";default:return e}},formatDateTime:function(e){return this.dayjs(e).format("YYYY-MM-DD HH:mm:ss")},getAlarMLogType:function(e){switch(e){case"DINGDING":return"钉钉";case"CALLBACK_URL":return"自定义回调http";default:return""}},getAlarmLogStatus:function(e){switch(e){case"FAIL":return"失败";case"SUCCESS":return"成功";default:return""}}}},g=c,f=(a("7169"),a("2877")),d=Object(f["a"])(g,o,n,!1,null,"0b82f02b",null);t["default"]=d.exports}}]);