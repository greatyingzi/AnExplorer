package dev.dworks.apps.anexplorer.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.adapter.TransferAdapter;
import dev.dworks.apps.anexplorer.common.RecyclerFragment;
import dev.dworks.apps.anexplorer.directory.DividerItemDecoration;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.service.TransferService;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ShareDeviceActivity;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;
import dev.dworks.apps.anexplorer.transfer.model.TransferStatus;

import static android.widget.LinearLayout.VERTICAL;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_BROADCAST;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_REMOVE_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_STATUS;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.TRANSFER_UPDATED;

public class TransferFragment extends RecyclerFragment
        implements TransferAdapter.OnItemClickListener, View.OnClickListener  {

    private static final String TAG = "TransferFragment";

    private TransferAdapter mAdapter;
    private String emptyText;
    private TransferHelper mTransferHelper;
    private TextView status;
    private Button action;

    public static void show(FragmentManager fm) {
        final TransferFragment fragment = new TransferFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static TransferFragment get(FragmentManager fm) {
        return (TransferFragment) fm.findFragmentByTag(TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        emptyText = getString(R.string.activity_transfer_empty_text);
        mTransferHelper = new TransferHelper(getActivity(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_transfer,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final Resources res = getActivity().getResources();

        // Indent our list divider to align with text
        final boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
        final int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
        DividerItemDecoration decoration = new DividerItemDecoration(getActivity(), VERTICAL);
        if (insetLeft) {
            decoration.setInset(insetSize, 0);
        } else {
            decoration.setInset(0, insetSize);
        }
        if(!isWatch()) {
            getListView().addItemDecoration(decoration);
        }
        setLayoutManager(new LinearLayoutManager(view.getContext()));
        setHasFixedSize(true);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(createItemTouchHelper());
        itemTouchHelper.attachToRecyclerView(getListView());


        ImageView icon = view.findViewById(R.id.icon);
        icon.setImageDrawable(IconUtils.applyTintAttr(getContext(), R.drawable.ic_root_transfer,
                android.R.attr.textColorPrimaryInverse));
        status = (TextView) view.findViewById(R.id.status);
        action = (Button) view.findViewById(R.id.action);
        action.setOnClickListener(this);

        setStatus(TransferHelper.isServerRunning(getActivity()));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();

        mAdapter = new TransferAdapter(context);
        mAdapter.setOnItemClickListener(this);
        setListAdapter(mAdapter);
        showRecyclerView();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TRANSFER_UPDATED);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

        Intent broadcastIntent = new Intent(getActivity(), TransferService.class);
        broadcastIntent.setAction(ACTION_BROADCAST);
        getActivity().startService(broadcastIntent);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_transfer, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_show_devices:
                Intent shareIntent = new Intent(getContext(), ShareDeviceActivity.class);
                getActivity().startActivity(shareIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showRecyclerView(){
        setListShown(true);
        if (mAdapter.getItemCount() == 0) {
            setEmptyText(emptyText);
        } else {
            setEmptyText("");
        }
    }

    @Override
    public void onItemClick(TransferAdapter.ViewHolder item, View view, int position) {

    }

    @Override
    public void onItemLongClick(TransferAdapter.ViewHolder item, View view, int position) {

    }

    @Override
    public void onItemViewClick(TransferAdapter.ViewHolder item, View view, int position) {

    }

    private ItemTouchHelper.Callback createItemTouchHelper() {
        ItemTouchHelper.Callback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TransferStatus transferStatus = mAdapter.get(position);
                mAdapter.remove(position);
                showRecyclerView();

                // Remove the item from the service
                Intent removeIntent = new Intent(getActivity(), TransferService.class);
                removeIntent.setAction(ACTION_REMOVE_TRANSFER);
                removeIntent.putExtra(EXTRA_TRANSFER, transferStatus.getId());
                getActivity().startService(removeIntent);
            }
        };
        return simpleCallback;
    }

    private BroadcastReceiver mBroadcastReceiver = new  BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TransferStatus transferStatus = intent.getParcelableExtra(EXTRA_STATUS);
            mAdapter.update(transferStatus);
            showRecyclerView();
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.action:
                if(!TransferHelper.isServerRunning(getActivity())){
                    setStatus(true);
                    mTransferHelper.startTransferServer();
                }
                else{
                    setStatus(false);
                    mTransferHelper.stopTransferServer();
                }
                break;
        }
    }

    private void setStatus(boolean running){
        if(running){
            status.setTextColor(SettingsActivity.getPrimaryColor());
            status.setText(getString(R.string.ftp_status_running));
            action.setText(R.string.stop_ftp);
        } else {
            status.setTextColor(SettingsActivity.getAccentColor());
            status.setText(getString(R.string.ftp_status_not_running));
            action.setText(R.string.start_ftp);
        }
    }
}