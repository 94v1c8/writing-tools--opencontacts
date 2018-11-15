package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.actions.ExportMenuItemClickHandler;
import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.fragments.CallLogFragment;
import opencontacts.open.com.opencontacts.fragments.ContactsFragment;
import opencontacts.open.com.opencontacts.fragments.DialerFragment;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;


public class MainActivity extends AppBaseActivity {
    public static final int CONTACTS_TAB_INDEX = 1;
    public static final String INTENT_EXTRA_LONG_CONTACT_ID = "contact_id";
    public static final String TAB_TITLE_CALL_LOG = "Call Log";
    public static final String TAB_TITLE_CONTACTS = "Contacts";
    private ViewPager viewPager;
    private SearchView searchView;
    private CallLogFragment callLogFragment;
    private ContactsFragment contactsFragment;
    private DialerFragment dialerFragment;

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_tabbed);
        AndroidUtils.askForPermissionsIfNotGranted(this);
        super.onCreate(savedInstanceState);
        setupTabs();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        menu.findItem(R.id.button_new).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent addContact = new Intent(MainActivity.this, EditContactActivity.class);
                addContact.putExtra(EditContactActivity.INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, true);
                startActivity(addContact);
                return false;
            }
        });
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnSearchClickListener(v -> {
            viewPager.setCurrentItem(CONTACTS_TAB_INDEX);
            searchView.requestFocus();
        });
        contactsFragment.configureSearchInMenu(searchView);
        menu.findItem(R.id.action_export).setOnMenuItemClickListener(new ExportMenuItemClickHandler(this));
        menu.findItem(R.id.action_about).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
        });
        menu.findItem(R.id.action_help).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(MainActivity.this, HelpActivity.class));
            return true;
        });
        menu.findItem(R.id.action_resync).setOnMenuItemClickListener(item -> {
            CallLogDataStore.updateCallLogAsyncForAllContacts(MainActivity.this);
            return true;
        });
        return super.onCreateOptionsMenu(menu);
    }
    private void refresh() {
        CallLogDataStore.loadRecentCallLogEntriesAsync(MainActivity.this);
    }

    private void setupTabs() {
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        callLogFragment = new CallLogFragment();
        contactsFragment = new ContactsFragment();
        dialerFragment = new DialerFragment();
        final List<SelectableTab> tabs = new ArrayList<>(Arrays.asList(callLogFragment, contactsFragment, dialerFragment));
        final List<String> tabTitles = Arrays.asList(TAB_TITLE_CALL_LOG, TAB_TITLE_CONTACTS, "");

        FragmentPagerAdapter fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return tabTitles.get(position);
            }

            @Override
            public Fragment getItem(int position) {
                return (Fragment) tabs.get(position);
            }
        };
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.setOffscreenPageLimit(3); //crazy shit with viewPager in case used with tablayout

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(2).setIcon(R.drawable.dial_pad);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabs.get(tab.getPosition()).onSelect();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tabs.get(tab.getPosition()).onUnSelect();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public void collapseSearchView(){
        searchView.onActionViewCollapsed();
    }
}