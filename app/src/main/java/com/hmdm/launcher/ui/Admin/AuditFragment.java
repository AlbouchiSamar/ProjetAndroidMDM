package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hmdm.launcher.R;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.AuditAdapter;

import java.util.ArrayList;
import java.util.List;

public class AuditFragment extends Fragment {
    private RecyclerView recyclerViewAudit;
    private ProgressBar progressBar;
    private AuditAdapter auditAdapter;
    private ServerApi serverService;
    private List<AuditFragment.AuditLog> auditLogs = new ArrayList<>();

    public static class AuditLog {
        private long createTime;
        private String login;
        private String ipAddress;
        private String action;

        public AuditLog(long createTime, String login, String ipAddress, String action) {
            this.createTime = createTime;
            this.login = login;
            this.ipAddress = ipAddress;
            this.action = action;
        }

        public long getCreateTime() { return createTime; }
        public String getLogin() { return login; }
        public String getIpAddress() { return ipAddress; }
        public String getAction() { return action; }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audit, container, false);

        recyclerViewAudit = view.findViewById(R.id.recycler_view_audit);
        progressBar = view.findViewById(R.id.progress_bar);

        recyclerViewAudit.setLayoutManager(new LinearLayoutManager(requireContext()));
        auditAdapter = new AuditAdapter(auditLogs);
        recyclerViewAudit.setAdapter(auditAdapter);

        loadAuditLogs();

        return view;
    }

    private void loadAuditLogs() {
        progressBar.setVisibility(View.VISIBLE);
        serverService.searchAuditLogs(1, 50, null, "", null, null,
                auditLogs -> requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    this.auditLogs.clear();
                    this.auditLogs.addAll(auditLogs);
                    auditAdapter.notifyDataSetChanged();
                }),
                error -> requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Erreur : " + error, Toast.LENGTH_LONG).show();
                }));
    }
}