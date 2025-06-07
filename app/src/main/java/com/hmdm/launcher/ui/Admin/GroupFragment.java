package com.hmdm.launcher.ui.Admin;



import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.hmdm.launcher.R;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class GroupFragment extends Fragment {

    private static final String TAG = "GroupFragment";
    private EditText editSearch, editGroupName;
    private ListView listGroups;
    private Button btnAddGroup;
    private ServerApi serverService;
    private List<Group> groupsList = new ArrayList<>();
    private ArrayAdapter<Group> adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        // Initialisation des vues
        editSearch = view.findViewById(R.id.edit_search);
        editGroupName = view.findViewById(R.id.edit_group_name);
        listGroups = view.findViewById(R.id.list_groups);
        btnAddGroup = view.findViewById(R.id.btn_add_group);

        // Initialisation du service
        serverService = new ServerServiceImpl(requireContext());

        // Initialisation de l'adaptateur
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, groupsList);
        listGroups.setAdapter(adapter);

        // Charger les groupes
        loadGroups();

        // Action de recherche
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchGroups(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Action du bouton ajouter
        btnAddGroup.setOnClickListener(v -> addGroup());

        // Action sur clic d'un groupe
        listGroups.setOnItemClickListener((parent, view1, position, id) -> showGroupOptions(position));

        return view;
    }

    private void loadGroups() {
        serverService.getGroups(new ServerApi.GetGroupsCallback() {
            @Override
            public void onGroupList(List<Group> groups) {
                requireActivity().runOnUiThread(() -> {
                    groupsList.clear();
                    groupsList.addAll(groups);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Erreur chargement groupes: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Erreur chargement groupes: " + error);
                });
            }
        });
    }

    private void searchGroups(String query) {
        serverService.searchGroups(query, new ServerApi.GetGroupsCallback() {
            @Override
            public void onGroupList(List<Group> groups) {
                requireActivity().runOnUiThread(() -> {
                    groupsList.clear();
                    groupsList.addAll(groups);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Erreur recherche groupes: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Erreur recherche groupes: " + error);
                });
            }
        });
    }

    private void addGroup() {
        String name = editGroupName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Le nom du groupe est requis", Toast.LENGTH_SHORT).show();
            return;
        }
        serverService.addGroup(name, new ServerApi.AddGroupCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Groupe ajouté avec succès", Toast.LENGTH_SHORT).show();
                    editGroupName.setText("");
                    loadGroups();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Erreur ajout groupe: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur ajout groupe: " + error);
                });
            }
        });
    }

    private void showGroupOptions(int position) {
        Group group = groupsList.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle("Options pour " + group.getName())
                .setItems(new String[]{"Modifier", "Supprimer"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(group);
                    } else {
                        deleteGroup(group.getId());
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showEditDialog(Group group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Modifier le groupe");
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_group, null);
        EditText editName = view.findViewById(R.id.edit_name);
        editName.setText(group.getName());
        builder.setView(view)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Le nom du groupe est requis", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateGroup(group.getId(), newName, group.getCustomerId(), group.getCommon());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateGroup(int id, String name, int customerId, boolean common) {
        if (id <= 0) {
            Toast.makeText(requireContext(), "ID de groupe invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        serverService.updateGroup(id, name, customerId, common, new ServerApi.AddGroupCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Groupe modifié avec succès", Toast.LENGTH_SHORT).show();
                    loadGroups();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Erreur modification groupe: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur modification groupe: " + error);
                });
            }
        });
    }

    private void deleteGroup(int id) {
        serverService.deleteGroup(id, new ServerApi.DeleteGroupCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Groupe supprimé avec succès", Toast.LENGTH_SHORT).show();
                    loadGroups();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Erreur suppression groupe: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur suppression groupe: " + error);
                });
            }
        });
    }

    // Classe interne pour représenter un groupe
    public static class Group {
        private int id;
        private String name;
        private int customerId;
        private boolean common;

        public Group(int id, String name, int customerId, boolean common) {
            this.id = id;
            this.name = name;
            this.customerId = customerId;
            this.common = common;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getCustomerId() {
            return customerId;
        }

        public boolean getCommon() {
            return common;
        }

        @Override
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }
}