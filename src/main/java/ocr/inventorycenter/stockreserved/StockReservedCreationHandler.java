package ocr.inventorycenter.stockreserved;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：预留 --创建预留
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockReservedCreationHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "reserved";

	public static final String ONHAND_REGISTER = "stockonhand-mgr.query";

	public StockReservedCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	// 处理器
	// 步骤1、根据传入参数{商品SKU+仓库编码}获取对应现存量
	// 步骤2、校验传入参数{拣货单.拣货数量} <= {步骤1.现存量- SUM{预留表{相同 商品SKU+仓库编码}}}。
	// 步骤3、如果步骤2 ok 则成功预留。否则预留拣货失败（目前整单预留，不进行部分预留）
	// -------------------
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
/*		JsonObject so = msg.body();
		
		String errString = stockOnHandNullVal(so);
		if (errString != null && !errString.equals("")) {
			msg.fail(100, "如下字段不能空："+ errString);
		}
		
		// 步骤1, 获取现存量
		this.appActivity.getEventBus().send(getOnHandAddress(), getOnHandParam(so), onhandservice -> {
			if (onhandservice.succeeded()) {
				JsonObject onhands = (JsonObject) onhandservice.result().body();
				if(onhands.isEmpty()){
					String errMsg = "未找到   " + so.getString(StockOnHandConstant.sku) + " 的现存量";
					msg.fail(100, errMsg);
				}
				
				
				
			    JsonArray results=	onhands.getJsonArray("result");
			    
			     Double onhandnum = 0.0;
			     
			    if(results!=null&&!results.isEmpty()){
			    	JsonObject onhand = results.getJsonObject(0);//TODO 汇总一下？
					
					 onhandnum = onhand.getDouble("onhandnum");
			    }
			  
			    final Double onhandnum2=onhandnum;
				
				Double pickoutnum = so.getDouble(StockReservedConstant.pickoutnum);
				
				// 步骤2、校验数量是否可以预留 根据 sku + warehousecode 查询预留量
				String sku = so.getString(StockReservedConstant.sku);
				String whcode = so.getString(StockReservedConstant.warehousecode);
				
				
				JsonObject queryObj = new JsonObject();
				queryObj.put(StockReservedConstant.sku, sku);
				queryObj.put(StockReservedConstant.warehousecode, whcode);				
				
				JsonObject fields = new JsonObject();
				fields.put("_id", false);
				fields.put(StockReservedConstant.pickoutnum, true);
				
				JsonObject queryMsg = new JsonObject();
				queryMsg.put("queryObj", queryObj);
				queryMsg.put("resFields", fields);
				
				//this.appActivity.getEventBus().send(this.appActivity.getAppInstContext().getAccount() +"."+ "ocr-inventorycenter.stockreserved.querySRNum", queryMsg,
				StockReservedNumQueryHandler srmQueryHandler = new StockReservedNumQueryHandler(this.appActivity);				
				srmQueryHandler.querySRNum(queryMsg, onQueryServerdNum -> {
					if(onQueryServerdNum.succeeded()){						
						JsonArray jArray = onQueryServerdNum.result();
						if(jArray != null && jArray.size() > 0){
							Double count = 0.0;
							for(Object item : jArray){
								count += ((JsonObject)item).getDouble(StockReservedConstant.pickoutnum);					
							}	
							if(pickoutnum > (onhandnum2 - count)){
								msg.fail(100, "预留拣货失败,库存不足");
							}
						}
						
						//步骤3、如果步骤2 ok 则成功预留。否则预留拣货失败（目前整单预留，不进行部分预留）
						StockReservedSaveHandler stockReservedSaveHandler = new StockReservedSaveHandler(this.appActivity);
						//this.appActivity.getEventBus().send(this.appActivity.getAppInstContext().getAccount() +"."+ "ocr-inventorycenter.stockreserved.saveStockReserved", so,onSaveStockReserved -> {
						stockReservedSaveHandler.saveStockReserved(so, onSaveStockReserved -> {								
							if(onSaveStockReserved.succeeded()){
								msg.reply(onSaveStockReserved.result());
							}
							else{
								Throwable err = onhandservice.cause();
								String errMsg = err.getMessage();
								componentImpl.getLogger().error(errMsg, err);
								msg.fail(100, errMsg);
							}
						});						
						
					}
					else{
						Throwable err = onQueryServerdNum.cause();
						String errMsg = err.getMessage();
						componentImpl.getLogger().error(errMsg, err);
						msg.fail(100, errMsg);
					}
				});
				
				
			} else {
				Throwable err = onhandservice.cause();
				String errMsg = err.getMessage();
				componentImpl.getLogger().error(errMsg, err);
				msg.fail(100, errMsg);
			}

		});
*/
	}


	private JsonObject getOnHandParam(JsonObject so) {
		JsonObject params = new JsonObject();
		params.put("sku", so.getString("sku"));
		params.put("warehousecode", so.getString("warehousecode"));
		
		if(so.containsKey("batch_code")){
			String batchCode = so.getString("batch_code");
			if(batchCode != null && !batchCode.isEmpty())
				params.put("invbatchcode", batchCode);	
		}
		
		params.put("goodaccount", so.getString("goodaccount"));
		return params;
	}

	private String getOnHandAddress() {
		String accountID = this.appActivity.getAppInstContext().getAccount();
/*		String authSrvName = this.appActivity.getDependencies().getJsonObject("stockonhand_service")
				.getString("service_name", "");*/
		String address =accountID + "."+ this.appActivity.getService().getRealServiceName() + "." + ONHAND_REGISTER;
		return address;
	}

	

	private String stockOnHandNullVal(JsonObject so) {
		StringBuffer errors = new StringBuffer();

		String warehouses = so.getString("warehousecode");
		if (null == warehouses || warehouses.equals("")) {
			errors.append("仓库");
		}

		String sku = so.getString("sku");
		if (null == sku || sku.equals("")) {
			errors.append("sku");
		}

/*		String goods = so.getString("goodaccount");
		if (null == goods || goods.equals("")) {
			errors.append("货主");
		}*/
		
		String pickoutid = so.getString("pickoutid");
		if (null == pickoutid || pickoutid.isEmpty()) {
			errors.append("拣货单号"); 
		}

		Double pickoutnum = so.getDouble("pickoutnum");
		if (null == pickoutnum || pickoutnum.equals(0.0)) {
			errors.append("拣货量"); 
		}

		return errors.toString();
	}


	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}
	

}
