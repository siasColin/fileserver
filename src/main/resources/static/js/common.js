var Common = {
    //当前项目名称
    ctxPath: "/",
    log: function (info) {
        console.log(info);
    },
    msg: function (info, iconIndex) {
        layui.use('layer', function() {
            var layer = layui.layer;
            layer.msg(info, {
                icon: iconIndex
            });
        });
    },
    alert: function (info, iconIndex) {
        layui.use('layer', function() {
            var layer = layui.layer;
            layer.alert(info, {
                icon: iconIndex,
                skin: 'layui-layer-molv'
            });
        });
    },
    load: function(){
        layui.use('layer', function() {
            var layer = layui.layer;
            layer.load();
        });
    },
    closeload: function(){
        layui.use('layer', function() {
            var layer = layui.layer;
            layer.closeAll('loading');
        });
    },
    info: function (info) {
        Common.msg(info, 0);
    },
    success: function (info) {
        Common.msg(info, 1);
    },
    error: function (info) {
        Common.openConfirm(info)
    },
    openConfirm:function(content,callback,callBackNo){
        layui.use('layer', function() {
            var index = layer.confirm(content, {
                btn: ['确认', '取消'] //按钮
            }, function () {
                if (callback != null) {
                    callback();
                }
                layer.close(index);
            }, function () {
                if (callBackNo != null) {
                    callBackNo()
                }
                layer.close(index);
            });
        });

    }
};
