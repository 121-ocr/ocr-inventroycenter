package ocr.inventorycenter.stockout;

import io.vertx.core.json.JsonArray;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 拣货出库单--（创建态-->拣货状）
 * 
 * @author LCL
 *
 */
public class StockOutBatchPickOutTESTHandler extends ActionHandlerImpl<JsonArray> {

	public StockOutBatchPickOutTESTHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonArray> event) {
		// TODO Auto-generated method stub
		
	}
}
