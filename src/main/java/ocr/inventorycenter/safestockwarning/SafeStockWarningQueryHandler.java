package ocr.inventorycenter.safestockwarning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.DoubleUtil;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 安全库存预警
 * 
 * @date 2016年12月10日
 * @author wanghw
 */
public class SafeStockWarningQueryHandler extends ActionHandlerImpl<JsonObject> {

	public SafeStockWarningQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return SafeStockWarningConstant.QUERY_ADDRESS;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	public String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 先查询安全库存，再查现存量，然后计算缺货量
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg){
		
		String from_account = this.appActivity.getAppInstContext().getAccount();
		String safeStockAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".safestock.query4warning";
		this.appActivity.getEventBus().send(safeStockAddress, null, invRet -> {
			if (invRet.succeeded()) {
				String onHandAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
						+ ".stockonhand-mgr.query4safestockwarning";
				this.appActivity.getEventBus().send(onHandAddress, null, ret -> {
					if (ret.succeeded()) {
						JsonObject result = (JsonObject) invRet.result().body();
						JsonArray safeStockList = result.getJsonArray("result");
						JsonArray onhandNumList = (JsonArray) ret.result().body();
						Map<String, Double> key2OnhandNum = new HashMap<>();
						onhandNumList.forEach(item->{
							Double onhandNum = ((JsonObject) item).getDouble("onhandnum");
							JsonObject _id = ((JsonObject) item).getJsonObject("_id");
							String warehousecode = _id.getString("warehousecode");
							String sku = _id.getString("sku");
							key2OnhandNum.put(warehousecode+sku, onhandNum);
						});
						safeStockList.forEach(item->{
							String warehousecode = ((JsonObject)item).getString("warehousecode");
							String sku = ((JsonObject)item).getString("sku");
							Double onhandNum = key2OnhandNum.get(warehousecode+sku);
							((JsonObject)item).put("onhandnum", onhandNum);
							Double safestockNum = Double.valueOf(((JsonObject)item).getValue("safenum").toString());
							if(onhandNum == null && safestockNum != null){
								((JsonObject)item).put("isWarning", "缺货");
								((JsonObject)item).put("stockoutnum", safestockNum);
							}else if(DoubleUtil.sub(onhandNum, safestockNum) < 0){
								((JsonObject)item).put("isWarning", "缺货");
								((JsonObject)item).put("stockoutnum", Math.abs(DoubleUtil.sub(onhandNum, safestockNum)));
							}else{
								((JsonObject)item).put("isWarning", "未缺货");
								((JsonObject)item).put("stockoutnum", 0.0);
							}
						});						
						msg.reply(safeStockList);
					} else {
						Throwable errThrowable = ret.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.reply(ret.cause());
					}
				});
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.reply(invRet.cause());
			}
		});
	}

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
