package ocr.inventorycenter.stockreserved;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.inventorycenter.stockonhand.StockOnHandConstant;
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
		JsonObject so = msg.body();
		
		
		if (stockOnHandNullVal(so) != null && !stockOnHandNullVal(so).equals("")) {
			msg.fail(100, stockOnHandNullVal(so));
		}
		
		// 步骤1, 获取现存量
		this.appActivity.getEventBus().send(getOnHandAddress(), getOnHandParam(so), onhandservice -> {
			if (onhandservice.succeeded()) {
				JsonArray onhands = (JsonArray) onhandservice.result().body();
				if(onhands.isEmpty()){
					String errMsg = "未找到   " + so.getString(StockOnHandConstant.sku) + " 的现存量";
					msg.fail(100, errMsg);
				}
				JsonObject onhand = onhands.getJsonObject(0);
				
				Double onhandnum = onhand.getDouble("onhandnum");
				
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
				
				
				this.appActivity.getEventBus().send(this.appActivity.getAppInstContext().getAccount() +"."+ "ocr-inventorycenter.stockreserverd.querySRNum", queryMsg,onQueryServerdNum -> {
					if(onQueryServerdNum.succeeded()){
						
						JsonArray jArray = (JsonArray) onQueryServerdNum.result().body();
						Double count = 0.0;
						for(int i=0;i<jArray.size();i++){
							count+=jArray.getJsonObject(i).getDouble(StockReservedConstant.pickoutnum);
						}
						if(pickoutnum > (onhandnum - count)){
							msg.fail(100, "预留拣货失败,库存不足");
						}else{
							//步骤3、如果步骤2 ok 则成功预留。否则预留拣货失败（目前整单预留，不进行部分预留）
							this.appActivity.getEventBus().send(this.appActivity.getAppInstContext().getAccount() +"."+ "ocr-inventorycenter.stockreserverd.saveStockReserved", so,onSaveStockReserved -> {
								if(onSaveStockReserved.succeeded()){
									msg.reply(onSaveStockReserved.result().body());
								}
								else{
									Throwable err = onhandservice.cause();
									String errMsg = err.getMessage();
									componentImpl.getLogger().error(errMsg, err);
									msg.fail(100, errMsg);
								}
							});
						}
						
					}
					else{
						Throwable err = onhandservice.cause();
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

	}


	private JsonObject getOnHandParam(JsonObject so) {
		JsonObject params = new JsonObject();
		params.put("sku", so.getString("sku"));
		params.put("warehousecode", so.getString("warehousecode"));
		params.put("goodaccount", so.getString("goodaccount"));
		return params;
	}

	private String getOnHandAddress() {
		String accountID = this.appActivity.getAppInstContext().getAccount();
		String authSrvName = this.appActivity.getDependencies().getJsonObject("stockonhand_service")
				.getString("service_name", "");
		String address =accountID + "."+ authSrvName + "." + ONHAND_REGISTER;
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

		String goods = so.getString("goodaccount");
		if (null == goods || goods.equals("")) {
			errors.append("商品");
		}

		Double reservednum = so.getDouble("reservednum");
		if (null == reservednum || reservednum.equals(0.0)) {
			errors.append("预留量");
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
