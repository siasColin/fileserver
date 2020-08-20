var fileTable;
var bigfileTable;
$(function(){
    layui.use([ 'upload', 'table','form'], function(){
        var upload = layui.upload
            ,table = layui.table
            ,form = layui.form
        //执行一个 table 实例
        fileTable = table.render({
            id: 'filetable'
            ,elem: '#filelist'
            ,height: "350"
            ,limit:5
            ,limits:[5,10,20,30,50]
            ,method:'GET'
            ,url: Common.ctxPath+'listFiles' //数据接口
            ,title: '小文件列表'
            ,page: true //开启分页
            ,toolbar: '#info_toolbar' //开启工具栏，此处显示默认图标，可以自定义模板，详见文档
            ,defaultToolbar: ['filter', 'print']
            ,totalRow: false //开启合计行
            ,cols: [[ //表头
                {type: 'checkbox', fixed: 'left'}
                ,{field: 'name', title: '文件名称',templet: function(d){
                    return '<a href="'+Common.ctxPath+'files/'+d.id+'" >'+d.name+'</a>';
                }}
                // ,{field: 'contentType', title: '文件类型'}
                ,{field: 'size', title: '文件大小',templet: function(d){
                        return '<span>'+Math.ceil(d.size/1024)+' KB</span>'
                 }}
                ,{field: 'uploadDate', title: '上传时间',templet: "<div>{{layui.util.toDateString(d.uploadDate, 'yyyy-MM-dd HH:mm:ss')}}</div>"}
                ,{fixed: 'right',  align:'center', toolbar: '#barFilelist',width:130}
            ]]
        });

        bigfileTable = table.render({
            id: 'bigfiletable'
            ,elem: '#bigfilelist'
            ,height: "350"
            ,limit:5
            ,limits:[5,10,20,30,50]
            ,method:'GET'
            ,url: Common.ctxPath+'listBigFiles' //数据接口
            ,title: '大文件列表'
            ,page: true //开启分页
            ,defaultToolbar: ['filter', 'print']
            ,totalRow: false //开启合计行
            ,cols: [[ //表头
                {field: 'fileName', title: '文件名称',templet: function(d){
                    return '<a href="'+Common.ctxPath+'bigFileDownload/'+d.fileId+'" >'+d.fileName+'</a>';
                }}
                ,{field: 'fileSize', title: '文件大小',templet: function(d){
                        return '<span>'+d.fileSize+' KB</span>'
                }}
                ,{field: 'uploadDate', title: '上传时间',templet: "<div>{{layui.util.toDateString(d.uploadTime, 'yyyy-MM-dd HH:mm:ss')}}</div>"}
                ,{fixed: 'right',  align:'center', toolbar: '#barFilelist',width:130}
            ]]
        });
        //监听行工具事件
        table.on('tool(filetable)', function(obj){
            var data = obj.data //获得当前行数据
                ,layEvent = obj.event; //获得 lay-event 对应的值
            if(layEvent === 'del'){
                Common.openConfirm("确定删除吗?",function () {
                    $.ajax({
                        url: Common.ctxPath+"file/" + data.id,
                        type: 'delete',
                        success:function(res){
                            if(res.returnCode == '0'){//成功
                                Common.success(res.returnMessage);
                                fileSearch();
                            }else{
                                Common.info(res.returnMessage);
                            }
                        },
                        error:function(){
                            Common.error("删除失败!")
                        }

                    });
                });
            }
        });
        table.on('tool(bigfiletable)', function(obj){
            var data = obj.data //获得当前行数据
                ,layEvent = obj.event; //获得 lay-event 对应的值
            if(layEvent === 'del'){
                Common.openConfirm("确定删除吗?",function () {
                    $.ajax({
                        url: Common.ctxPath+"bigFile/" + data.fileId,
                        type: 'delete',
                        success:function(res){
                            if(res.returnCode == '0'){//成功
                                Common.success(res.returnMessage);
                                bigFileSearch();
                            }else{
                                Common.info(res.returnMessage);
                            }
                        },
                        error:function(){
                            Common.error("删除失败!")
                        }

                    });
                });
            }
        });
        //监听头工具栏事件
        table.on('toolbar(filetable)', function(obj){
            var checkStatus = table.checkStatus(obj.config.id)
                ,data = checkStatus.data; //获取选中的数据
            switch(obj.event){
                case 'delete':
                    if(data.length === 0){
                        Common.info("请至少选择一行");
                    } else {
                        var ids = "";
                        for(var i=0;i<data.length;i++){
                            if(i == data.length - 1){
                                ids+=data[i].id
                            }else{
                                ids+=data[i].id+',';
                            }
                        }
                        Common.openConfirm("确定删除吗?",function () {
                            $.ajax({
                                url: Common.ctxPath+"files/" + ids,
                                type: 'delete',
                                success:function(res){
                                    if(res.returnCode == '0'){//成功
                                        Common.success(res.returnMessage);
                                        fileSearch();
                                    }else{
                                        Common.info(res.returnMessage);
                                    }
                                },
                                error:function(){
                                    Common.error("删除失败!")
                                }

                            });
                        });
                    }
                    break;
            };
        });
        uploadInit(upload);
    });
})
function search(){
    fileSearch();
    bigFileSearch();
}

function fileSearch(){
    var name = $('#name');
    fileTable.reload({
        page: {
            curr: 1 //重新从第 1 页开始
        }
        ,where: {
            name: name.val()
        }
    });
    layui.use([ 'upload'], function() {
        var upload = layui.upload
        uploadInit(upload);
    });
}
function bigFileSearch() {
    var name = $('#name');
    bigfileTable.reload({
        page: {
            curr: 1 //重新从第 1 页开始
        }
        ,where: {
            name: name.val()
        }
    });
}

function uploadInit(uploadobj){
    //单文件上传
    var uploadInst = uploadobj.render({
        elem: '#uploadFile'
        ,url: Common.ctxPath+'singleUpload'
        ,accept: 'file' //普通文件
        ,field:'file'
        ,before: function () {
            Common.load();
        }
        ,done: function(res){
            Common.closeload();
            if(res.returnCode == '0'){//成功
                Common.success("上传成功");
                search();
            }else{
                Common.info(res.returnMessage);
            }
        }
        ,error: function(){
            Common.closeload();
            Common.info("请求异常");
        }
    });
    //多文件上传
    var multipleUploadInst = uploadobj.render({
        elem: '#multipleUploadFile'
        ,url: Common.ctxPath+'multipleUpload'
        ,accept: 'file' //普通文件
        ,multiple: true
        ,field:'files'
        ,before: function () {
            Common.load();
        }
        ,done: function(res){
            Common.closeload();
            if(res.returnCode == '0'){//成功
                Common.success("上传成功");
                search();
            }else{
                Common.info(res.returnMessage);
            }
        }
        ,error: function(){
            Common.closeload();
            Common.info("请求异常");
        }
    });
}