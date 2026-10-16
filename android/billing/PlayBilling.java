/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.Activity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

/**
 * Thanh toán thật qua Google Play Billing (thư viện 8.x), sản phẩm "Bản Đầy Đủ" mua một lần (INAPP, không tiêu hao).
 * Chỉ được biên dịch trong quy trình build dành cho Google Play (android/gradle), nơi tải được thư viện này.
 * Được nạp qua Class.forName trong Billing.get(), nên bản APK cài trực tiếp không cần tới lớp này.
 */
public final class PlayBilling extends Billing implements PurchasesUpdatedListener {
    private BillingClient client;
    private ProductDetails details;
    private String err = "";
    private boolean connecting;

    public PlayBilling() {
    }

    private BillingClient client() {
        if (client == null) {
            client = BillingClient.newBuilder(app)
                    .setListener(this)
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                    .enableAutoServiceReconnection()
                    .build();
        }
        return client;
    }

    @Override
    void connect(Activity a) {
        final BillingClient c = client();
        if (c.isReady()) {
            loadDetails();
            queryOwned();
            return;
        }
        if (connecting) return;
        connecting = true;
        c.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult r) {
                connecting = false;
                if (r.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    err = "";
                    loadDetails();
                    queryOwned();
                } else {
                    err = "Chưa kết nối được Google Play (" + r.getResponseCode() + ").";
                    changed();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                connecting = false;
            }
        });
    }

    private void loadDetails() {
        QueryProductDetailsParams p = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildInfo.PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()))
                .build();
        client().queryProductDetailsAsync(p, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(BillingResult r, QueryProductDetailsResult res) {
                List<ProductDetails> list = res == null ? null : res.getProductDetailsList();
                if (r.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                    details = list.get(0);
                } else if (r.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    err = "Chưa tìm thấy sản phẩm trên Google Play.";
                }
                changed();
            }
        });
    }

    /** Kiểm tra các giao dịch đã mua. Thành công mà không có giao dịch nào (hoàn tiền, đổi tài khoản) thì khóa lại. */
    private void queryOwned() {
        client().queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(BillingResult r, List<Purchase> list) {
                        if (r.getResponseCode() != BillingClient.BillingResponseCode.OK) return; // mất mạng: giữ nguyên trạng thái đã lưu
                        boolean owned = false;
                        for (Purchase p : list) {
                            if (p.getProducts().contains(BuildInfo.PRODUCT_ID) && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                owned = true;
                                acknowledge(p);
                            }
                        }
                        Pro.set(app, owned);
                        changed();
                    }
                });
    }

    /** Google tự hoàn tiền nếu không xác nhận (acknowledge) giao dịch trong 3 ngày. */
    private void acknowledge(Purchase p) {
        if (p.isAcknowledged()) return;
        client().acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(),
                new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(BillingResult r) {
                    }
                });
    }

    @Override
    void buy(Activity a) {
        if (details == null) {
            err = "Chưa tải được thông tin sản phẩm. Hãy kiểm tra mạng rồi thử lại.";
            connect(a);
            changed();
            return;
        }
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
                .build();
        BillingResult r = client().launchBillingFlow(a, params);
        if (r.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            err = "Không mở được màn hình thanh toán (" + r.getResponseCode() + ").";
            changed();
        }
    }

    @Override
    void restore(Activity a) {
        connect(a);
    }

    @Override
    public void onPurchasesUpdated(BillingResult r, List<Purchase> list) {
        int code = r.getResponseCode();
        if (code == BillingClient.BillingResponseCode.OK && list != null) {
            for (Purchase p : list) {
                if (p.getProducts().contains(BuildInfo.PRODUCT_ID) && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    acknowledge(p);
                    Pro.set(app, true);
                }
            }
            err = "";
        } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryOwned();
        } else if (code != BillingClient.BillingResponseCode.USER_CANCELED) {
            err = "Thanh toán chưa thành công (" + code + ").";
        }
        changed();
    }

    @Override
    String price() {
        if (details == null || details.getOneTimePurchaseOfferDetails() == null) return "";
        return details.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    @Override
    String error() {
        return err;
    }

    @Override
    boolean available() {
        return true;
    }
}
