package ocr.inventorycenter.stockonhand;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：现存量查询，货位参照
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockOnHandQueryBySkuHandler extends ActionHandlerImpl<JsonObject> {
    
	//查询方法中间变量
	public static final String ADDRESS = "query_avaliable";
	public static final String onhandArray = "onhandArray";
	public static final String locationArray = "locationArray";
	public static final String reversedArray = "reversedArray";
	// 前台传递参数：
	public static final String sku = "sku";
	public static final String warehouse = "warehouse";
	// results 结果集合：
	public static final String results = "results";
	public static final String batchcode = "batchcode";
	public static final String location = "location";
	public static final String onhandnum = "onhandnum";
	public static final String resevednum = "resevednum";
	public static final String locationnum = "locationnum";

	public StockOnHandQueryBySkuHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> event) {

		getLocationsBySku(event, event.body(), ret -> {

			if (ret.succeeded()) {
				event.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}

	private void getLocationsBySku(OtoCloudBusMessage<JsonObject> mgs, JsonObject bo,
			Handler<AsyncResult<JsonArray>> next) {

		// 步骤1：根据sku+仓库 获取改物料所有绑定，固定仓位及对应满载量 并行
		// 步骤2：步骤1--步骤2 串行：根据 [仓库+仓位 ] 批量获取所有现存量数据（sku + 现存量、批次、仓位）并行
		// 步骤3、步骤1--步骤3 串行：根据 [仓库+仓位 ] 批量获取所有预留数据（sku +预留量、批次、仓位）并行
		// 步骤4、（合并步骤1、2、3 结果),循环步骤1仓位集合，
		// 如果步骤2有数据，根据key[批次+sku+仓位+仓库]得到最终[对应现存量、预留量、满载量]
		// 如果步骤2没有数据，该仓位前面没有对应存储物料
		// ------------
		List<Future> futures = new ArrayList<Future>();

		JsonObject resultObjects = new JsonObject();
		resultObjects.put("sku", bo.getString("sku"));
		resultObjects.put("warehouse", bo.getString("warehouse"));
		JsonArray resultArray = new JsonArray();

		//由于需要等到步骤二、三都执行完后再执行合并数据逻辑，故需要定义两个future
		Future<JsonObject> returnFuture1 = Future.future();
		futures.add(returnFuture1);
		Future<JsonObject> returnFuture2 = Future.future();
		futures.add(returnFuture2);


		Future<Void> nextFuture1 = Future.future(); // 黄色部分，规定步骤1、2之间的顺序
		//Future<Void> nextFuture2 = Future.future(); // 黄色部分，规定步骤1、2之间的顺序

		//因为步骤二、三即便放在一起调用也是异步的，故只需要一个future即可
		nextFuture1.setHandler(nextHandler -> {
			// 步骤二，与步骤一串行的，并且结束后通过returnFurture通知外面future
			getOnHandByLcations(bo, resultObjects, returnFuture1);
			// 步骤三，与步骤一串行的，并且结束后通过returnFurture通知外面future
			getResevedByLcations(bo, resultObjects, returnFuture2);
		});

/*		nextFuture2.setHandler(nextHandler -> {
			// 步骤三，与步骤一串行的，并且结束后通过returnFurture通知外面future
			getResevedByLcations(bo, resultObjects, returnFuture);
		});*/

		// 步骤一,批量产品仓位对应关系
		getLocationByGoods(bo, resultObjects, nextFuture1);

		CompositeFuture.join(futures).setHandler(ar -> { // 合并所有for循环结果，返回外面
			CompositeFutureImpl comFutures = (CompositeFutureImpl) ar;
			if (comFutures.size() > 0) {
				for (int i = 0; i < comFutures.size(); i++) {
					if (comFutures.succeeded(i)) {
						JsonObject itemObject = comFutures.result(i);
						JsonArray onhands = itemObject.getJsonArray(onhandArray);
						JsonArray reverseds = itemObject.getJsonArray(reversedArray);
						JsonArray locations = itemObject.getJsonArray(locationArray);
						for (Object locationObject : locations) {
							JsonObject locationObj = (JsonObject) locationObject;
							String locationvalue = locationObj.getString(location);
							Double locationnumvalue = locationObj.getDouble(locationnum);
							// 现存量没有数据，那么就是此货位目前为空
							if (!onhands.contains(location)) {

								JsonObject resultObject = new JsonObject();
								resultObject.put(sku, ""); // 没有放任何sku
								resultObject.put(batchcode, ""); // 批次空
								resultObject.put(location, locationvalue); // 仓位
								resultObject.put(locationnum, locationnumvalue);// 仓位满载数量
								resultObject.put(onhandnum, 0.0);// 现存量
								resultObject.put(resevednum, 0.0);// 预留量
								resultArray.add(resultObject);
								resultObjects.put(results, resultArray);
							} else {

								for (Object onhandObjects : onhands) {
									JsonObject onhandObject = (JsonObject) onhandObjects;
									JsonObject resultObject = new JsonObject();
									resultObjects.put(sku, onhandObject.getString(sku)); // 目前存放的sku
									resultObjects.put(batchcode, onhandObject.getString(batchcode)); // 批次号
									resultObjects.put(location, locationvalue); // 仓位
									resultObjects.put(locationnum, locationnumvalue);// 仓位满载数量
									resultObjects.put(onhandnum, onhandObject.getString(onhandnum));// 现存量
									resultObjects.put(resevednum, getReserved(onhandObject, reverseds));// 根据key维度，得到对应预留量
									resultArray.add(resultObject);

								}
								resultObjects.put(results, resultArray);
							}

						}

					}
				}
			}

			mgs.reply(resultObjects);
		});
	}

	private Double getReserved(JsonObject onhands, JsonArray reverseds) {

		String key = onhands.getString(sku) + onhands.getString(batchcode) + onhands.getString(location);

		Double reversennum = 0.0;
		for (Object reversedObj : reverseds) {
			JsonObject reversed = (JsonObject) reversedObj;
			String key2 = reversed.getString(sku) + reversed.getString(batchcode) + reversed.getString(location);
			if (key.equals(key2)) {
				reversennum = reversed.getDouble(resevednum);
				break;

			}

		}

		return reversennum;
	}

	private void getResevedByLcations(JsonObject params, JsonObject resultObjects, Future<JsonObject> returnFuture) {
      //不同组件还是需要sent
		this.appActivity.getEventBus().send(getReservedAddress(), getReservedCond(params), res -> {
			if (res.succeeded()) {
				JsonArray reverseds = (JsonArray) res.result().body();
				resultObjects.put(reversedArray, reverseds);
				returnFuture.complete();

			} else {
				Throwable err = res.cause();
				String errMsg = err.getMessage();
				appActivity.getLogger().error(errMsg, err);
				returnFuture.failed();

			}

		});

	}

	private void getOnHandByLcations(JsonObject params, JsonObject resultObjects, Future<JsonObject> returnFuture) {
        //同一个组件，new 即可
		StockOnHandQueryByLocationHandler hander = new StockOnHandQueryByLocationHandler(this.appActivity);
		hander.getLocationsByRule(getParamByFIFO(params), res -> {
			if (res.succeeded()) {
				JsonArray onhands = (JsonArray) res.result();
				resultObjects.put(onhandArray, onhands);
				returnFuture.complete();
			} else {
				Throwable err = res.cause();
				String errMsg = err.getMessage();
				appActivity.getLogger().error(errMsg, err);
				returnFuture.failed();
			}

		});

	}

	private void getLocationByGoods(JsonObject params, JsonObject resultObjects, Future<Void> nextFuture) {

		this.appActivity.getEventBus().send(getLocationGoodsRelAddress(), getLocationGoodsRelCond(params),
				facilityRes -> {
					if (facilityRes.succeeded()) {
						JsonArray locations = (JsonArray) facilityRes.result().body();
						resultObjects.put(locationArray, locations);
						nextFuture.complete();
						//nextFuture2.complete();
					} else {
						Throwable err = facilityRes.cause();
						String errMsg = err.getMessage();
						appActivity.getLogger().error(errMsg, err);
						nextFuture.failed();
						//nextFuture2.complete();
					}

				});
	}

	private String getLocationGoodsRelAddress() {
		String facilitySer = this.appActivity.getService().getRealServiceName();
		String facilityAddress = this.appActivity.getAppInstContext().getAccount() + "." + facilitySer + "." + "."
				+ InvBusiOpenContant.FACILITYCOMPONTENNAME + "." + InvBusiOpenContant.LOCATIONSADDRESS;
		return facilityAddress;
	}

	private String getReservedAddress() {
		String facilitySer = this.appActivity.getService().getRealServiceName();
		String facilityAddress = this.appActivity.getAppInstContext().getAccount() + "." + facilitySer + "." + "."
				+ InvBusiOpenContant.RESERVEDCOMPONTENNAME + "." + InvBusiOpenContant.QUERYRESERVEDSADDRESS;
		return facilityAddress;
	}

	private JsonObject getLocationGoodsRelCond(JsonObject params) {
		JsonObject queryLocationGoodsRelCondObject = new JsonObject();
		queryLocationGoodsRelCondObject.put(sku, params.getString(sku));
		return queryLocationGoodsRelCondObject;
	}

	private JsonObject getReservedCond(JsonObject params) {
		JsonObject queryCondition = new JsonObject();
		queryCondition.put(StockOnHandConstant.warehousecode, params.getString(warehouse));
		queryCondition.put(StockOnHandConstant.locationcode, params.getString(location));
		return queryCondition;
	}

	private JsonObject getParamByFIFO(JsonObject param) {

		// 根据 [仓库+仓位 ] 批量获取所有现存量数据（sku + 现存量、批次、仓位 ）并行

		JsonObject queryObj = new JsonObject();
		queryObj.put(StockOnHandConstant.warehousecode, param.getString(warehouse));
		queryObj.put(StockOnHandConstant.locationcode, param.getString(location));

		JsonObject params = new JsonObject();
		params.put("query", queryObj);

		JsonArray groupKeys = new JsonArray();
		groupKeys.add(StockOnHandConstant.warehousecode).add(StockOnHandConstant.sku)
				.add(StockOnHandConstant.invbatchcode).add(StockOnHandConstant.locationcode);
		params.put("groupKeys", groupKeys);
		// TODO 还需要稍微改造下 此获取fifo方法
		return params;
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
