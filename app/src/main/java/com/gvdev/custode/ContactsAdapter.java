package com.gvdev.custode;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ContactsAdapter extends ArrayAdapter<ContactsAdapter.ContactItem> implements Filterable {

    private Filter filter;
    private List<ContactItem> unfilteredItems;
    private static final int resource = android.R.layout.simple_list_item_multiple_choice;

    public static class ContactItem {
        public final String name;
        public final String number;
        public boolean checked;

        public ContactItem(String name, String number, boolean checked) {
            this.name = name;
            this.number = number;
            this.checked = checked;
        }
    }

    public ContactsAdapter(Context context, List<ContactItem> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(resource, null);

        ContactItem contact = getItem(position);
        if (contact != null) {
            String html = "<b>" + contact.name + "</b> " + contact.number;
            Spanned newText;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                newText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
            else
                newText = Html.fromHtml(html);

            CheckedTextView textView = (CheckedTextView) convertView;
            textView.setText(newText);
            textView.setChecked(contact.checked);
            textView.jumpDrawablesToCurrentState(); // (!!!) Ferma l'animazione dovuta a setChecked, terribile quando vengono riciclate le view
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new ContactsAdapterFilter();
        return filter;
    }


    private class ContactsAdapterFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            if (unfilteredItems == null) {
                unfilteredItems = new ArrayList<>();
                for (int i = 0; i < getCount(); i++)
                    unfilteredItems.add(getItem(i));
            }

            String query = charSequence.toString().toLowerCase();
            FilterResults filterResults = new FilterResults();

            if (query.isEmpty()) {
                filterResults.count = unfilteredItems.size();
                filterResults.values = unfilteredItems;
                return filterResults;
            }

            LinkedList<ContactItem> resultsList = new LinkedList<>();
            int startsWithLimit = 0; // usato per ordinare in cima i nomi che *iniziano* con query
            for (int i = 0; i < getCount(); i++) {
                ContactItem contactItem = getItem(i);
                String contactItemName = contactItem.name.toLowerCase();

                if (contactItemName.startsWith(query))
                    resultsList.add(startsWithLimit++, contactItem);
                else if (contactItemName.contains(query))
                    resultsList.add(contactItem);
            }

            filterResults.count = resultsList.size();
            filterResults.values = resultsList;
            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            List<ContactItem> resultsList = (List<ContactItem>) filterResults.values;
            clear();
            addAll(resultsList);
            if (resultsList.size() > 0)
                notifyDataSetChanged();
            else
                notifyDataSetInvalidated();
        }
    }

}
