package opencontacts.open.com.opencontacts.data.datastore;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;


/**
 * Created by sultanm on 8/5/17.
 */

class CallLogDBHelper {
    private Map<String, Integer> simsInfo = null;

    private void createSimsInfo(Context context) {
        simsInfo = new HashMap<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            List<SubscriptionInfo> activeSubscriptionInfoList = ((SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)).getActiveSubscriptionInfoList();
            if(activeSubscriptionInfoList == null)
                return;
            for(SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList){
                simsInfo.put(subscriptionInfo.getIccId(), subscriptionInfo.getSimSlotIndex() + 1);
            }
        }
    }
    private String preferenceLastCallLogSavedDate = "preference_last_call_log_saved_date";

    public List<CallLogEntry> loadRecentCallLogEntriesIntoDB(Context context) {
        List<CallLogEntry> callLogEntries = getRecentCallLogEntries(context);
        CallLogEntry.saveInTx(callLogEntries);
        return callLogEntries;
    }

    private List<CallLogEntry> getRecentCallLogEntries(final Context context){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Handler mainHandler = new Handler(context.getMainLooper());

            mainHandler.post(() -> Toast.makeText(context, R.string.grant_read_call_logs_permission, Toast.LENGTH_LONG).show());
            return new ArrayList<>(0);
        }
        Cursor c;
        String mobileNumberInvolvedInCall, dateOfCall, durationOfCall, callType, subscriptionIdForCall;
        ArrayList<CallLogEntry> callLogEntries = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1){//TODO: refactor below two if else blocks 90% same
            createSimsInfo(context);
            c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.PHONE_ACCOUNT_ID}, CallLog.Calls.DATE + " > ?", new String[]{getLastSavedCallLogDate(context)}, CallLog.Calls.DATE + " DESC");
            if(c.getCount() == 0)
                return callLogEntries;
            int columnIndexForNumber = c.getColumnIndex(CallLog.Calls.NUMBER);
            int columnIndexForDuration = c.getColumnIndex(CallLog.Calls.DURATION);
            int columnIndexForDate = c.getColumnIndex(CallLog.Calls.DATE);
            int columnIndexForCallType = c.getColumnIndex(CallLog.Calls.TYPE);
            int columnIndexForSubscriptionId = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID);
            while(c.moveToNext()){
                mobileNumberInvolvedInCall = c.getString(columnIndexForNumber);// for  number
                durationOfCall = c.getString(columnIndexForDuration);// for duration
                dateOfCall = c.getString(columnIndexForDate);
                callType = c.getString(columnIndexForCallType);// for call type, Incoming or out going
                subscriptionIdForCall = c.getString(columnIndexForSubscriptionId);
                opencontacts.open.com.opencontacts.orm.Contact contact = ContactsDataStore.getContact(mobileNumberInvolvedInCall);
                if(contact == null)
                    callLogEntries.add(new CallLogEntry(null, (long)-1, mobileNumberInvolvedInCall, durationOfCall, callType, dateOfCall, getSimIdOrDefault(subscriptionIdForCall)));
                else
                    callLogEntries.add(new CallLogEntry(contact.toString(), contact.getId(), mobileNumberInvolvedInCall, durationOfCall, callType, dateOfCall, getSimIdOrDefault(subscriptionIdForCall)));
            }
            c.moveToFirst();
            setLastSavedCallLogDate(c.getString(columnIndexForDate), context);
            c.close();
        }
        else {
            c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.TYPE, CallLog.Calls.DATE}, CallLog.Calls.DATE + " > ?", new String[]{getLastSavedCallLogDate(context)}, CallLog.Calls.DATE + " DESC");
            if(c.getCount() == 0)
                return callLogEntries;
            int columnIndexForNumber = c.getColumnIndex(CallLog.Calls.NUMBER);
            int columnIndexForDuration = c.getColumnIndex(CallLog.Calls.DURATION);
            int columnIndexForDate = c.getColumnIndex(CallLog.Calls.DATE);
            int columnIndexForCallType = c.getColumnIndex(CallLog.Calls.TYPE);
            while(c.moveToNext()){
                mobileNumberInvolvedInCall = c.getString(columnIndexForNumber);// for  number
                durationOfCall = c.getString(columnIndexForDuration);// for duration
                dateOfCall = c.getString(columnIndexForDate);
                callType = c.getString(columnIndexForCallType);// for call type, Incoming or out going

                opencontacts.open.com.opencontacts.orm.Contact contact = ContactsDataStore.getContact(mobileNumberInvolvedInCall);
                if(contact == null)
                    callLogEntries.add(new CallLogEntry(null, (long)-1, mobileNumberInvolvedInCall, durationOfCall, callType, dateOfCall, 1));
                else
                    callLogEntries.add(new CallLogEntry(contact.toString(), contact.getId(), mobileNumberInvolvedInCall, durationOfCall, callType, dateOfCall, 1));
            }
            c.moveToFirst();
            setLastSavedCallLogDate(c.getString(columnIndexForDate), context);
            c.close();
        }
        return callLogEntries;
    }

    private int getSimIdOrDefault(String subscriptionIdForCall) {
        Integer simId = simsInfo.get(subscriptionIdForCall);
        return simId == null ? 1 : simId;
    }

    private String getLastSavedCallLogDate(Context context) {
        return AndroidUtils.getAppsSharedPreferences(context).getString(preferenceLastCallLogSavedDate, "0");
    }

    private void setLastSavedCallLogDate(String date, Context context) {
        AndroidUtils.getAppsSharedPreferences(context).edit().putString(preferenceLastCallLogSavedDate, date).apply();
    }

    public static List<CallLogEntry> getRecent100CallLogEntriesFromDB(){
        return CallLogEntry.find(CallLogEntry.class, null, null, null, "date desc", "100");
    }
}
