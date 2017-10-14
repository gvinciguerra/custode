package com.gvdev.custode.activities;

import android.Manifest;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.gvdev.custode.ContactsAdapter;
import com.gvdev.custode.CustodeUtils;
import com.gvdev.custode.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Un'activity che permette di selezionare una insieme di numeri di telefono presi dai contatti
 * dell'utente e che salva la selezione in SharedPreferences.
 */
public class ContactsPickerActivity extends AppCompatActivity implements
        SearchView.OnQueryTextListener,
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private ListView listView;
    private ContactsAdapter contactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_picker);
        listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 0);
        else
            loadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contacts_picker_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.contacts_search).getActionView();
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        contactsAdapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            loadData();
        else
            finish();
    }

    private void loadData() {
        getLoaderManager().initLoader(0, null, this);
        contactsAdapter = new ContactsAdapter(this, new ArrayList<ContactsAdapter.ContactItem>());
        listView.setAdapter(contactsAdapter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Salva i contatti selezionati
        listView.getCount();
        int items = contactsAdapter.getCount();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> checkedNumbersSet = new HashSet<>();
        for (int i = 0; i < items; i++) {
            ContactsAdapter.ContactItem contact = contactsAdapter.getItem(i);
            if (contact.checked)
                checkedNumbersSet.add(contact.number);
        }
        sharedPreferences.edit().putStringSet(SettingsActivity.PREFERENCES_CONTACTS_KEY, checkedNumbersSet).apply();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME;

        return new CursorLoader(this, ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        contactsAdapter.clear();

        Set<String> favoriteContacts = CustodeUtils.getFavoriteContacts(this);
        LinkedList<ContactsAdapter.ContactItem> tempArray = new LinkedList<>();
        int favoriteLimit = 0; // usato per ordinare i contatti preferiti in cima alla lista

        int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        while (cursor.moveToNext()) {
            String name = cursor.getString(nameColumnIndex);
            String number = cursor.getString(numberColumnIndex);

            boolean favorite = favoriteContacts.remove(number);
            if (favorite)
                tempArray.add(favoriteLimit++, new ContactsAdapter.ContactItem(name, number, true));
            else
                tempArray.addLast(new ContactsAdapter.ContactItem(name, number, false));
        }

        if (favoriteContacts.size() > 0) // => favoriteContacts contiene numeri che non sono più presenti in rubrica
            for (String favoriteContact : favoriteContacts)
                tempArray.addFirst(new ContactsAdapter.ContactItem("", favoriteContact, true));

        contactsAdapter.addAll(tempArray);
    }


    @Nullable
    @Override
    public Intent getParentActivityIntent() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        return intent;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        contactsAdapter.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ContactsAdapter.ContactItem contactItem = (ContactsAdapter.ContactItem) adapterView.getItemAtPosition(i);
        contactItem.checked = !contactItem.checked;
        ((CheckedTextView) view).setChecked(contactItem.checked);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = NavUtils.getParentActivityIntent(this);
            TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
