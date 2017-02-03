package ocr.inventorycenter.stockonhand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

	private static final String FIXEDTYPE = "fixed";
	private static final String FREETYPE = "free";
	// 查询方法中间变量
	public static final String ADDRESS = "query_avaliable";
	public static final String onhandArray = "onhandArray";
	public static final String locationArray = "locationArray";
	public static final String reversedArray = "reversedArray";
	// 前台传递参数：
	public static final String SKU = "sku";
	public static final String warehouse = "warehouse";
	// results 结果集合：
	public static final String results = "results";
	public static final String batchcode = "invbatchcode";
	public static final String location = "locationcode";
	public static final String onhandnum = "onhandnum";
	public static final String resevednum = "resevednum";
	public static final String plusnum = "plusnum";
	public static final String locationnum = "locationnum";
	public static final String packageunit = "packageunit";
	public static final String warehousecode = "warehousecode";
	public static final String sheftscode = "sheftscode";

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

		getLocationsBySku(event.body(), ret -> {

			if (ret.succeeded()) {
				event.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}

	// 步骤1：根据sku+仓库 获取改物料所有绑定，固定仓位及对应满载量 并行
	// 步骤2：步骤1--步骤2 串行：根据 [仓库+仓位 ] 批量获取所有现存量数据（sku + 现存量、批次、仓位）并行
	// 步骤3、步骤1--步骤3 串行：根据 [仓库+仓位 ] 批量获取所有预留数据（sku +预留量、批次、仓位）并行
	// 步骤4、（合并步骤1、2、3 结果),循环步骤1仓位集合，
	// 如果步骤2有数据，根据key[批次+sku+仓位+仓库]得到最终[对应现存量、预留量、满载量]
	// 如果步骤2没有数据，该仓位前面没有对应存储物料
	// ------------
	public void getLocationsBySku(JsonObject bo, Handler<AsyncResult<JsonArray>> next) {

		Future<JsonArray> res = Future.future();
		res.setHandler(next);
		List<Future> futures = new ArrayList<Future>();

		JsonObject resultObjects = new JsonObject();

		JsonObject query = bo.getJsonObject("query");
		String sku = query.getString("sku");
		String whCode = query.getString("warehousecode");
		Boolean fixedType = query.getString("type").equals(FIXEDTYPE) ? true : false;

		resultObjects.put("sku", sku);
		resultObjects.put("warehouse", whCode);
		JsonArray resultArray = new JsonArray();

		// 由于需要等到步骤二、三都执行完后再执行合并数据逻辑，故需要定义两个future
		Future<JsonObject> returnFuture1 = Future.future();
		futures.add(returnFuture1);

		// Future<JsonObject> returnFuture2 = Future.future();
		// futures.add(returnFuture2);

		Future<Void> nextFuture = Future.future(); // 黄色部分，规定步骤1、2之间的顺序

		// 因为步骤二、三即便放在一起调用也是异步的，故只需要一个future即可
		nextFuture.setHandler(nextHandler -> {
			// 步骤二，与步骤一串行的，并且结束后通过returnFurture通知外面future
			getOnHandByLcations(bo, resultObjects, returnFuture1);
			// 步骤三，与步骤一串行的，并且结束后通过returnFurture通知外面future，先先
			// getResevedByLcations(bo, resultObjects, returnFuture2);
		});

		// 步骤一,批量产品仓位对应关系

		getLocations(fixedType, sku, whCode, resultObjects, nextFuture);

		CompositeFuture.join(futures).setHandler(ar -> { // 合并所有for循环结果，返回外面
			CompositeFutureImpl comFutures = (CompositeFutureImpl) ar;
			if (comFutures.size() > 0) {
				for (int i = 0; i < comFutures.size(); i++) {
					if (comFutures.succeeded(i)) {
						JsonObject itemObject = comFutures.result(i);
						JsonArray onhands = itemObject.getJsonArray(onhandArray);
						// JsonArray reverseds =
						// itemObject.getJsonArray(reversedArray);
						JsonArray locations = itemObject.getJsonArray(locationArray);
						for (Object locationObject : locations) {
							JsonObject locationObj = (JsonObject) locationObject;
							String locationvalue = locationObj.getString(location);
							Double locationnumvalue = 0.0;
							if (locationObj.containsKey(locationnum)) {
								locationnumvalue = locationObj.getDouble(locationnum);
							}
							// 现存量没有数据，那么就是此货位目前为空
							if (onhands == null || onhands.size() == 0) {
								JsonObject resultObject = new JsonObject();
								resultObject.put(SKU, ""); // 没有放任何sku
								resultObject.put(batchcode, ""); // 批次空
								resultObject.put(location, locationvalue); // 仓位
								resultObject.put(locationnum, locationnumvalue);// 仓位满载数量
								resultObject.put(packageunit, locationObj.getString(packageunit));// 单位
								resultObject.put(warehousecode, locationObj.getString(warehousecode));
								resultObject.put(onhandnum, 0.0);// 现存量
								resultObject.put(resevednum, 0.0);// 预留量
								resultObject.put(plusnum, locationnumvalue);// 剩余数量
								resultArray.add(resultObject);

								continue;
							}
							for (Object onhandObjects : onhands) {
								JsonObject onhandObject = (JsonObject) onhandObjects;
								JsonObject onhandObject2 = (JsonObject) onhandObject.getValue("_id");
								if (!onhandObject2.getString(location).equals(locationvalue)) {
									continue;
								}
								JsonObject resultObject = new JsonObject();
								resultObject.put(packageunit, locationObj.getString(packageunit));// 单位
								resultObject.put(SKU, onhandObject2.getString(SKU)); // 目前存放的sku
								resultObject.put(batchcode, onhandObject2.getString(batchcode)); // 批次号
								resultObject.put(location, locationvalue); // 仓位
								resultObject.put(warehousecode, locationObj.getString(warehousecode));
								resultObject.put(locationnum, locationnumvalue);// 仓位满载数量
								resultObject.put(onhandnum, onhandObject.getDouble(onhandnum));// 现存量
								resultObject.put(resevednum, 0.0);// 根据key维度，得到对应预留量
								resultObject.put(plusnum, locationnumvalue - onhandObject.getDouble(onhandnum));// 剩余数量
								// resultObject.put(sheftscode,locationObj.getString(sheftscode)
								// );// hh

								// resultObject.put(resevednum,
								// getReserved(onhandObject2, reverseds));//
								// 根据key维度，得到对应预留量
								resultArray.add(resultObject);

							}

						}

					}

				}
				res.complete(sort2(resultArray));
			} else {
				Throwable err = ar.cause();
				String errMsg = err.getMessage();
				appActivity.getLogger().error(errMsg, err);
				res.fail(err);
			}

		});
	}

	private void getLocations(Boolean fixedType, String sku, String whCode, JsonObject resultObjects,
			Future<Void> nextFuture) {

		if (fixedType) { // 固定货位，根据货位和商品对应关系寻找货位
			getLocationByGoods(sku, whCode, resultObjects, nextFuture);

		} else {// 自由货位，根据货架标识（=散货），获取下面所有的货位。
			getLocationByShift(sku, whCode, resultObjects, nextFuture);

		}
	}

	/*
	 * private boolean isFreeType(JsonObject bo) { return ((JsonObject)
	 * bo.getJsonObject("query")).getString("type").equals(FREETYPE); }
	 */

	private void getLocationByShift(String sku, String whCode, JsonObject resultObjects, Future<Void> nextFuture) {

		this.appActivity.getEventBus().send(getSheftsGoodsRelAddress(), getSheftGoodsRelCond(sku, whCode),
				facilityRes -> {
					if (facilityRes.succeeded()) {
						JsonArray locations = (JsonArray) facilityRes.result().body();
						resultObjects.put(locationArray, locations);
						nextFuture.complete();

					} else {
						Throwable err = facilityRes.cause();
						String errMsg = err.getMessage();
						appActivity.getLogger().error(errMsg, err);
						nextFuture.failed();

					}

				});

	}

	private boolean isFixedType(JsonObject bo) {
		return ((JsonObject) bo.getJsonObject("query")).getString("type").equals(FIXEDTYPE);
	}

	private JsonArray sort2(JsonArray resultArray) {

		JsonArray sortedJsonArray = new JsonArray();

		List<JsonObject> jsonValues = new ArrayList<JsonObject>();
		for (int i = 0; i < resultArray.size(); i++) {
			jsonValues.add(resultArray.getJsonObject(i));
		}

		Collections.sort(jsonValues, new Comparator<JsonObject>() {

			@Override
			public int compare(JsonObject a, JsonObject b) {
				Double valA = 0.0;
				Double valB = 0.0;

				try {
					valA = a.getDouble(plusnum);// 剩余数量

					valB = a.getDouble(plusnum);// 剩余数量

				} catch (Exception e) {

				}

				return valA.compareTo(valB);

			}
		});

		for (int i = 0; i < jsonValues.size(); i++) {
			sortedJsonArray.add(jsonValues.get(i));
		}
		return sortedJsonArray;
	}

	/**
	 * 预留方法先不使用
	 * 
	 * @param onhands
	 * @param reverseds
	 * @return
	 * @deprecated
	 */
	private Double getReserved(JsonObject onhands, JsonArray reverseds) {

		String onhandkey = onhands.getString(SKU) + onhands.getString(batchcode) + onhands.getString(location);

		Double reversennum = 0.0;

		if (null == reverseds || reverseds.size() == 0) {
			return reversennum;
		}
		for (Object reversedObj : reverseds) {
			JsonObject reversed2 = (JsonObject) reversedObj;
			JsonObject reversed = (JsonObject) reversed2.getValue("_id");
			String reversekey = reversed.getString(SKU) + reversed.getString(batchcode) + reversed.getString(location);
			if (onhandkey.equals(reversekey)) {
				Double reversennum2 = reversed.getDouble(resevednum);
				if (reversennum2 == null) {
					reversennum = 0.0;
					break;
				}

				reversennum = reversennum2;
				break;

			}

		}

		return reversennum;
	}

	/**
	 * 先不考虑预留了
	 * 
	 * @param reqParams
	 * @param resultObjects
	 * @param returnFuture
	 * @deprecated
	 */
	private void getResevedByLcations(JsonObject reqParams, JsonObject resultObjects, Future<JsonObject> returnFuture) {

		// 根据 [仓库+仓位 ] 批量获取所有预留数据（sku + 现存量、批次、仓位 ）并行

		JsonObject queryParams = new JsonObject();

		// 设置参与计算的库存状态
		queryParams.put("status", new JsonArray().add("RES"));

		// 设置分组
		queryParams.put("group_keys",
				new JsonArray().add("warehousecode").add("locationcode").add("sku").add("invbatchcode"));

		// 设置排序
		queryParams.put("sort", new JsonObject().put("onhandnum", 1));

		JsonArray locations = resultObjects.getJsonArray(locationArray);
		JsonObject queryInv = new JsonObject();

		if (locations.size() == 1) {
			queryInv.put("locationcode", locations.getJsonObject(0).getString(location));
		} else {
			JsonArray queryItems = new JsonArray();
			queryInv.put("$or", queryItems);

			for (Object locationObj : locations) {
				JsonObject reversed = (JsonObject) locationObj;
				queryItems.add(new JsonObject().put("locationcode", reversed.getString(location)));
			}
		}

		queryParams.put("query", queryInv);

		// 调用现存量查询服务
		StockOnHandQueryHandler stockOnHandQueryHandler = new StockOnHandQueryHandler(this.appActivity);
		stockOnHandQueryHandler.queryOnHand(queryParams, res -> {
			if (res.succeeded()) {
				JsonArray onhands = res.result();
				resultObjects.put(reversedArray, onhands);
				returnFuture.complete(resultObjects);
			} else {
				Throwable err = res.cause();
				String errMsg = err.getMessage();
				appActivity.getLogger().error(errMsg, err);
				returnFuture.failed();
			}

		});

	}

	private void getOnHandByLcations(JsonObject reqParams, JsonObject resultObjects, Future<JsonObject> returnFuture) {

		// 根据 [仓库+仓位 ] 批量获取所有现存量数据（sku + 现存量、批次、仓位 ）并行

		JsonObject queryParams = new JsonObject();

		// 设置参与计算的库存状态
		queryParams.put("status", new JsonArray().add("IN").add("OUT"));

		// 设置分组
		queryParams.put("group_keys",
				new JsonArray().add("warehousecode").add("locationcode").add("sku").add("invbatchcode"));

		// 设置排序
		queryParams.put("sort", new JsonObject().put("onhandnum", 1));

		JsonArray locations = resultObjects.getJsonArray(locationArray);
		JsonObject queryInv = new JsonObject();

		if (locations.size() == 1) {
			queryInv.put(location, locations.getJsonObject(0).getString(location));
			if (isFixedType(reqParams)) {
				queryInv.put(SKU, ((JsonObject) reqParams.getJsonObject("query")).getString(SKU));
				queryInv.put(warehousecode, ((JsonObject) reqParams.getJsonObject("query")).getString(warehousecode));
			}

		} else {
			JsonArray queryItems = new JsonArray();
			queryInv.put("$or", queryItems);

			for (Object locationObj : locations) {
				JsonObject reversed = (JsonObject) locationObj;
				JsonObject item = new JsonObject();
				item.put(location, reversed.getString(location));
				item.put(warehousecode, reqParams.getJsonObject("query").getString(warehousecode));
				if (isFixedType(reqParams)) {
					item.put(SKU, reqParams.getJsonObject("query").getString(SKU));
				}

				queryItems.add(item);

			}
		}

		queryParams.put("query", queryInv);

		// 调用现存量查询服务
		StockOnHandQueryHandler stockOnHandQueryHandler = new StockOnHandQueryHandler(this.appActivity);
		stockOnHandQueryHandler.queryOnHand(queryParams, res -> {
			if (res.succeeded()) {
				JsonArray onhands = res.result();
				resultObjects.put(onhandArray, onhands);
				returnFuture.complete(resultObjects);
			} else {
				Throwable err = res.cause();
				String errMsg = err.getMessage();
				appActivity.getLogger().error(errMsg, err);
				returnFuture.failed();
			}

		});

	}

	private void getLocationByGoods(String sku, String whCode, JsonObject resultObjects, Future<Void> nextFuture) {

		this.appActivity.getEventBus().send(getLocationGoodsRelAddress(), getLocationGoodsRelCond(sku, whCode),
				facilityRes -> {
					if (facilityRes.succeeded()) {
						Object body = facilityRes.result().body();
						if (body != null) {
							JsonArray locations = (JsonArray) body;
							resultObjects.put(locationArray, locations);
							nextFuture.complete();
						} else {
							nextFuture.complete();

						}

					} else {
						Throwable err = facilityRes.cause();
						String errMsg = err.getMessage();
						appActivity.getLogger().error(errMsg, err);
						nextFuture.failed();

					}

				});
	}

	private String getLocationGoodsRelAddress() {
		String facilitySer = this.appActivity.getService().getRealServiceName();
		String facilityAddress = this.appActivity.getAppInstContext().getAccount() + "." + facilitySer + "."
				+ InvBusiOpenContant.LOCATIONRELATIONCOMPONTENNAME + "." + InvBusiOpenContant.LOCATIONSADDRESS;
		return facilityAddress;
	}

	private String getSheftsGoodsRelAddress() {
		String facilitySer = this.appActivity.getService().getRealServiceName();
		String facilityAddress = this.appActivity.getAppInstContext().getAccount() + "." + facilitySer + "."
				+ InvBusiOpenContant.SHEFTSRELATIONCOMPONTENNAME + "." + InvBusiOpenContant.SHEFTSLOCATIONSADDRESS;
		return facilityAddress;
	}

	private JsonObject getLocationGoodsRelCond(String sku, String whCode) {
		JsonObject queryLocationGoodsRelCondObject = new JsonObject();
		queryLocationGoodsRelCondObject.put("sku", sku);
		queryLocationGoodsRelCondObject.put("allotLocations.warehousecode", whCode);
		return queryLocationGoodsRelCondObject;
	}

	private JsonObject getSheftGoodsRelCond(String sku, String whCode) {
		JsonObject cond = new JsonObject();
		cond.put("type", "1");// 标识=散货
		return cond;
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
