package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.airbnb.lottie.LottieAnimationView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import java.util.ArrayList;

public class DeviceStatsFragment extends Fragment {

    private ServerApi serverApi;
    private TextView tvTotalDevices, tvEnrolledDevices, tvLastMonthEnrolled;
    private PieChart pieChart;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar loadingAnimation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        serverApi = new ServerServiceImpl(requireContext());

        tvTotalDevices = view.findViewById(R.id.tv_total_devices);
        tvEnrolledDevices = view.findViewById(R.id.tv_enrolled_devices);
        tvLastMonthEnrolled = view.findViewById(R.id.tv_last_month_enrolled);
        pieChart = view.findViewById(R.id.pie_chart);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        loadingAnimation = view.findViewById(R.id.loading_animation);

        setupPieChart();
        swipeRefreshLayout.setOnRefreshListener(this::refreshStats);
        refreshStats();
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.R.color.transparent);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setHoleRadius(40f);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(android.R.color.black);
        pieChart.getLegend().setEnabled(true);
    }

    private void refreshStats() {
        swipeRefreshLayout.setRefreshing(false);
        loadingAnimation.setVisibility(View.VISIBLE);

        String token = SettingsHelper.getInstance(requireContext()).getAdminAuthToken();
        if (token == null || token.isEmpty()) {
            Intent intent = new Intent(requireContext(), AdminLoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        } else {
            fetchDeviceStats(token);
        }
    }

    private void fetchDeviceStats(String token) {
        serverApi.getDeviceStats(
                token,
                (totalDevices, enrolledDevices, lastMonthEnrolled) ->
                        requireActivity().runOnUiThread(() -> {
                            tvTotalDevices.setText("Total: " + totalDevices);
                            tvEnrolledDevices.setText("Inscrits: " + enrolledDevices);
                            tvLastMonthEnrolled.setText("Inscrits dernier mois: " + lastMonthEnrolled);
                            updatePieChart(enrolledDevices, lastMonthEnrolled);
                            loadingAnimation.setVisibility(View.GONE);
                        }),
                error -> requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    loadingAnimation.setVisibility(View.GONE);
                })
        );
    }

    private void updatePieChart(int enrolled, int lastMonthEnrolled) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (enrolled > 0) entries.add(new PieEntry(enrolled, "Inscrits"));
        if (lastMonthEnrolled > 0) entries.add(new PieEntry(lastMonthEnrolled, "Inscrits dernier mois"));

        PieDataSet dataSet = new PieDataSet(entries, "Statistiques des appareils");
        dataSet.setColors(new int[]{0xFF4CAF50, 0xFFFF9800}, requireContext());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(android.R.color.white);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}