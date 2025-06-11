package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
        pieChart.setHoleColor(Color.parseColor("#0F172A")); // Dark blue
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setHoleRadius(40f);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.parseColor("#F8FAFC")); // Off-white
        pieChart.getLegend().setTextColor(Color.parseColor("#94A3B8")); // Light grey
        pieChart.getLegend().setTextSize(12f);
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
                            tvEnrolledDevices.setText("Enrolled: " + enrolledDevices);
                            tvLastMonthEnrolled.setText("Enrolled Last Month: " + lastMonthEnrolled);
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
        if (enrolled > 0) entries.add(new PieEntry(enrolled, "Enrolled"));
        if (lastMonthEnrolled > 0) entries.add(new PieEntry(lastMonthEnrolled, "Enrolled Last Month"));

        PieDataSet dataSet = new PieDataSet(entries, "Device Statistics");
        dataSet.setColors(new int[]{
                Color.parseColor("#4ADE80"), // Pastel mint green for enrolled
                Color.parseColor("#38BDF8")  // Soft sky blue for last month
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#F8FAFC")); // Off-white

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}