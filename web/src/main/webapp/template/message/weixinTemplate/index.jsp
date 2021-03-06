<!DOCTYPE HTML>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="../../common/import-tags.jsp"%>
<html>
<head>
	<title><spring:eval expression="@webConf['admin.title']" /></title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<%@include file="../../common/import-static.jsp"%>
	<style>
		.text-size{
			height: 60px;
			width: 500px;
		}
	</style>
</head>
<body>
	<div class="container">
		<!-- 查询 -->
		<form id="searchForm" class="form-horizontal search-form">
		<div class="row">
			<div class="control-group span7">
				<label class="control-label">模板code:</label>
				<div class="controls">
					<input type="text" maxlength="50" class="input-normal control-text" name="weixinCode">
				</div>
			</div>
			<div class="control-group span7">
				<label class="control-label">微信消息名称:</label>
				<div class="controls">
					<input type="text" maxlength="100" class="input-normal control-text" name="title">
				</div>
			</div>
			<div class="span1 offset2">
			  <button  type="button" id="btnSearch" class="button button-primary">搜索</button>
			</div>
			<div class="span1 offset2">
			  <button class="button button-danger" onclick="flushall();">清空</button>
			</div>
		</div>
		</form>
		<!-- 修改新增 -->
		<div id="addOrUpdate" class="hide">
			<form id="addOrUpdateForm" role="form" class="form-horizontal">
				<input type="hidden" name="id" value="">
				<div class="row">
					<div class="control-group span10">
						<label for="weixinCode" class="control-label">模板code</label>
						<div class="controls">
							<input type="text" class="form-control" id="weixinCode" maxlength="10"data-rules="{required:true,}"
								   style="width:100%" name="weixinCode" data-rules="{required:true,}"placeholder="模板code">
						</div>
					</div>
					<div class="control-group span10">
						<label for="title" class="control-label">微信消息名称</label>
						<div class="controls">
							<input type="text" class="form-control" id="title" name="title" style="width:100%"
								   data-rules="{required:true,}" maxlength="20" placeholder="微信消息名称">
						</div>
					</div>
				</div>
				<div class="row">
					<div class="control-group span24">
						<label for="weixinTempId" class="control-label">消息模板id</label>
						<div class="controls">
							<input type="text" class="form-control" style="width:350px" name="weixinTempId"
								   data-rules="{required:true,}" maxlength="100" id="weixinTempId" placeholder="消息模板id">
						</div>
					</div>
				</div>
				<div class="row">
					<div class="control-group span24">
						<label for="tempContent" class="control-label">模板内容</label>
						<div class="controls control-row6">
							<textarea class="form-control text-size"maxlength="300" data-rules="{required:true,}"
									  name="tempContent" id="tempContent"rows="6"></textarea>
						</div>
					</div>
				</div>
				<br/><br/>
				<div class="row">
					<div class="control-group span24">
						<label for="tempFirstDetail" class="control-label">模板的首行内容</label>
						<div class="controls control-row6">
							<textarea class="form-control text-size" data-rules="{required:true,}"
									  maxlength="200" name="tempFirstDetail" id="tempFirstDetail"rows="6"></textarea>
						</div>
					</div>
				</div>
				<br/><br/>
				<div class="row">
					<div class="control-group span8">
						<label for="keyword1" class="control-label">模板关键字1</label>
						<div class="controls">
							<input name = "keyword1" id="keyword1" maxlength="50"
								   data-rules="{required:true,}" type="text" class="input-normal control-text">
						</div>
					</div>
					<div class="control-group span8">
						<label for="keyword2" class="control-label">模板关键字2</label>
						<div class="controls">
							<input name = "keyword2" maxlength="50" id="keyword2" type="text"
								   class="input-normal control-text">
						</div>
					</div>
					<div class="control-group span8">
						<label for="keyword3" class="control-label">模板关键字3</label>
						<div class="controls">
							<input name = "keyword3" maxlength="50" id="keyword3" type="text"
								   class="input-normal control-text">
						</div>
					</div>
				</div>
				<div class="row">
					<div class="control-group span8">
						<label for="keyword4" class="control-label">模板关键字4</label>
						<div class="controls">
							<input name = "keyword4" id="keyword4" maxlength="50" type="text"
								   class="input-normal control-text">
						</div>
					</div>
					<div class="control-group span8">
						<label for="keyword5" class="control-label">模板关键字5</label>
						<div class="controls">
							<input name = "keyword5" id="keyword5" maxlength="50" type="text"
								   class="input-normal control-text">
						</div>
					</div>
				</div>
				<div class="row">
					<div class="control-group span24">
						<label for="tempRemarkDetail" class="control-label">模板备注</label>
						<div class="controls">
							<input name = "tempRemarkDetail" style="width: 550px" id="tempRemarkDetail" type="text"
								   maxlength="100" class="input-normal control-text">
						</div>
					</div>
				</div>
			</form>
		</div>
		<div class="search-grid-container">
			<div id="grid"></div>
		</div>
	</div>

<script type="text/javascript">

function flushall(){
	    var elementsByTagName = document.getElementsByTagName("input");
	    for(var i = 0;i<elementsByTagName.length;i++){
            elementsByTagName[i].innerText = "";
		}
	}

BUI.use(['bui/ux/crudgrid'],function (CrudGrid) {
	
	//定义页面权限
	var add=true,update=true,del=true;
	//"framwork:crudPermission"会根据用户的权限给add，update，del,list赋值
	<framwork:crudPermission resource="/message/weixinTemplate"/>

    var columns = [
		{title:'模板code',dataIndex:'weixinCode'},
		{title:'微信消息说明',dataIndex:'title'},
		{title:'消息模板id',dataIndex:'weixinTempId'},
        {title:'消息模板内容',dataIndex:'tempContent',width: '20%'},
        {title:'首行内容',dataIndex:'tempFirstDetail',width: '10%'},
        {title:'关键字1',dataIndex:'keyword1',width: '10%'},
        {title:'关键字2',dataIndex:'keyword2',width: '10%'},
        {title:'关键字3',dataIndex:'keyword3',width: '10%'},
        {title:'备注内容',dataIndex:'tempRemarkDetail',width: '20%'},
        {title:'创建时间',dataIndex:'createdTime'},
        {title:'更新时间',dataIndex:'updateTime'}
        ];
    
	var crudGrid = new CrudGrid({
		entityName : '微信消息模板',
    	pkColumn : 'id',//主键
      	storeUrl : '/message/weixinTemplate/list',
        addUrl : '/message/weixinTemplate/add',
        updateUrl : '/message/weixinTemplate/update',
        removeUrl : '/message/weixinTemplate/delete',
        columns : columns,
		showAddBtn : add,
		showUpdateBtn : update,
		showRemoveBtn : del,
		addOrUpdateFormId : 'addOrUpdateForm',
		dialogContentId : 'addOrUpdate',
		gridCfg:{
    		innerBorder:true
    	},
		storeCfg:{//定义store的排序，如果是复合主键一定要修改
			sortInfo : {
				field : 'code',//排序字段
				direction : 'DESC' //升序ASC，降序DESC
				}
			}
		});
});
 
</script>
</body>
</html>


