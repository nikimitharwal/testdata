public class SendReportActivity extends ViserSendReportActivity
implements FamillyPagerDialogFragment.FamilyDialogInterface {
    private Type        selectedType;
    private SousFamille selectedSousFamille;
    private Famille     selectedFamilly;
    private Boolean alreadyAskedPhoto;
    private Boolean alreadyAskedAddress;
    private Button validerSignalement;
    private ArrayList<Photo> modifyPhotos;
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onServiceConnected(OsisService service) {
    }
    @Override
    protected String getGAPageName() {
        return GAConstant.PAGE_CREER_SIGNALEMENT_ETAPEFINALE;
    }
    @Override
    protected void setHeaderAndFooter() {
        alreadyAskedPhoto = false;
        alreadyAskedAddress = false;
        setContentView(R.layout.activity_create_report);
        activityView = findViewById(R.id.layout_create_report);
        getIntent().getSerializableExtra("MyClass");
        if (address != null) {
            ((TextView) activityView.findViewById(R.id.address)).setText(address);
        }
        activityView.findViewById(R.id.additional_fields_cell_clickable)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendGAEvent(GAConstant.CAT_BASIC_EVENT,
                                GAConstant.ACTION_EVENT_CLIC,
                                GAConstant.EVENT_CREATION_SIGNALEMENT_CELL_INFOS_COMPLEMENTAIRES);
                        Intent intent = new Intent(SendReportActivity.this,
                                AdditionalScrollViewFieldsActivity.class);
                        startActivity(intent);
                    }
                });
        activityView.findViewById(R.id.famillyAndTypeLayout)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /*
                        sendGAEvent(GAConstant.CAT_BASIC_EVENT,
                                GAConstant.ACTION_EVENT_CLIC,
                                GAConstant.EVENT_CREATION_SIGNALEMENT_CELL_INFOS_COMPLEMENTAIRES);
                         */
                        FragmentManager fm = getSupportFragmentManager();
                        FamillyPagerDialogFragment famillyPagerDialogFragment = FamillyPagerDialogFragment.newInstance(SendReportActivity.this);
                        famillyPagerDialogFragment.show(fm, "Topology");
                    }
                });
        if(reportActionMasse != null) {
            reportAction = reportActionMasse;
            isModifyReport = true;
            selectedType = reportAction.getType();
            selectedSousFamille = selectedType.sousFamille(getApplicationContext());
            selectedFamilly = selectedSousFamille.famille(getApplicationContext());
            if(reportAction.getPhotos() != null) {
                images.add(reportAction.getPhotos());
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File picture = new File(storageDir, reportAction.getPhotos());
                Logger.d(picture.getAbsolutePath());
                try {
                    URI uri = new URI(picture.getAbsolutePath());
                    Uri newUri = Uri.parse(uri.toString());
                    listImage.add(newUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            address = reportAction.getAddress();
        } else {
            if (reportAction != null && reportAction.getAdditionalFields() != null) {
                JSONArray array = null;
                try {
                    array = new JSONArray(reportAction.getAdditionalFields());
                    for (int i = 0; i < array.length(); i++) {
                        AdditionalFieldsSetter.getInstance()
                                .setValueForFieldId(array.getJSONObject(i)
                                                .getInt("champComplementaire_id"),
                                        array.getJSONObject(i)
                                                .getString("valeur"));
                        ((TextView) activityView.findViewById(R.id.nb_AdditionalFieldsSetted)).setText(
                                AdditionalFieldsSetter.getInstance().getStat(getApplicationContext()));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        validerSignalement = activityView.findViewById(R.id.button_validate);
        validerSignalement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCreation()) {
                    sendGAEvent(GAConstant.CAT_BASIC_EVENT,
                            GAConstant.ACTION_EVENT_CLIC,
                            GAConstant.EVENT_CREATION_SIGNALEMENT_BTN_VALIDER);
                } else {
                    sendGAEvent(GAConstant.CAT_BASIC_EVENT,
                            GAConstant.ACTION_EVENT_CLIC,
                            GAConstant.EVENT_FICHE_SIGNALEMENT_VISER_BTN_VALIDER);
                }
                if (MpmPreferences.getInstance(getApplicationContext())
                        .isFakeGpsEnabled() && !isfakeGpsPossible()) {
                    CToast.makeText(SendReportActivity.this,
                            "Vous ne disposez pas des droits pour créer un signalement avec le GPS forcé" + targetGroup != null ? (" pour le groupe \"" + targetGroup
                                    .getLibelleGroupe() + "\"") : "",
                            Toast.LENGTH_SHORT).show();
                } else {
                    validate();
                }
            }
        });
        // 0 étant la valeur par défaut du type
        if (selectedType.getIdType() == DAL.getType(0, this).getIdType()) {
            validerSignalement.setClickable(false);
            validerSignalement.setFocusable(false);
            validerSignalement.setBackgroundColor(getResources().getColor(R.color.md_grey_200));
        }
        // TODO : Tester que le timeSlot fonctionne bien
        if (isModifyReport) {
            newFamily(selectedFamilly.getIdFamille(), selectedSousFamille.getIdSousFamille(), selectedType.getIdType());
            if (reportActionMasse != null && reportActionMasse.getAdditionalFields() != null) {
                try {
                    JSONArray array = new JSONArray(reportActionMasse.getAdditionalFields());
                    for (int i = 0; i < array.length(); i++) {
                        AdditionalFieldsSetter.getInstance()
                                .setValueForFieldId(array.getJSONObject(i)
                                                .getInt("champComplementaire_id"),
                                        array.getJSONObject(i)
                                                .getString("valeur"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                if (signalement != null && signalement.getChampsAdditionnel() > 0) {
                    signalement.setAdditionalFieldsValues(DAL.getAdditionalFieldsValue(signalement.getIdSignalement(),
                            SendReportActivity.this));
                    refreshAdditionalFieldValues();
                }
            }
            validerSignalement.setText(getResources().getString(R.string.modify));
            if(reportActionMasse == null) {
                setTitle("Modifier " + signalement.getIdSignalement());
                // Desactivation du changement d'adresse parce que non modifiable
                activityView.findViewById(R.id.recapButtonGeolocalisationS5).setVisibility(View.GONE);
                // Desactivation de la topologie parce que non modifiable
                activityView.findViewById(R.id.modifyTextView).setVisibility(View.GONE);
                activityView.findViewById(R.id.famillyAndTypeLayout).setClickable(false);
                activityView.findViewById(R.id.famillyAndTypeLayout).setFocusable(false);
            }
        }
    }
    public void refreshAdditionalFieldValues() {
        new AdditionalFieldValuesDownloader(this,
                signalement.getIdSignalement(),
                new AdditionalFieldValuesDownloader.AdditionalFieldsValueDownloaderDelegate() {
                    @Override
                    public void additionalFieldValuesDownloaderDidFinish(
                            StateCode stateCode) {
                        if (stateCode == StateCode.STATE_OK) {
                            signalement.setAdditionalFieldsValues(
                                    DAL.getAdditionalFieldsValue(
                                            signalement.getIdSignalement(),
                                            getApplicationContext()));
                            for (AdditionalFieldValue value : signalement
                                    .getAdditionalFieldsValues()) {
                                AdditionalFieldsSetter.getInstance()
                                        .setValueForFieldId(
                                                value.fieldId,
                                                value.value);
                            }
                            ((TextView) activityView.findViewById(R.id.nb_AdditionalFieldsSetted)).setText(
                                    AdditionalFieldsSetter.getInstance().getStat(getApplicationContext()));
                        }
                    }
                }).downloadAndStoreData();
    }
    private void setAdditionnalFieldsAndTimeSlot() {
        if (AdditionalFieldsSetter.getInstance().getAdditionalFieldsSize() == 0) {
            activityView.findViewById(R.id.additional_fields_cell).setVisibility(View.GONE);
            activityView.findViewById(R.id.timeSlot_cell).setVisibility(View.GONE);
        } else {
            activityView.findViewById(R.id.additional_fields_cell).setVisibility(View.VISIBLE);
            if (AdditionalFieldsSetter.getInstance().hasCompteurField()) {
                activityView.findViewById(R.id.timeSlot_cell).setVisibility(View.VISIBLE);
            } else {
                activityView.findViewById(R.id.timeSlot_cell).setVisibility(View.GONE);
            }
            if (AdditionalFieldsSetter.getInstance().hasMandatoryFields()) {
                ((TextView) activityView.findViewById(R.id.AdditionalFields)).setText(getResources().getString(R.string.additional_fields) + " *");
            }
        }
        if (AdditionalFieldsSetter.getInstance().getAdditionalFieldsSize() > 0) {
            ((TextView) activityView.findViewById(R.id.nb_AdditionalFieldsSetted)).setText(
                    AdditionalFieldsSetter.getInstance().getStat(getApplicationContext()));
        }
        activityView.findViewById(R.id.timeSlot_cell).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendGAEvent(GAConstant.CAT_BASIC_EVENT,
                        GAConstant.ACTION_EVENT_CLIC,
                        GAConstant.EVENT_CREATION_SIGNALEMENT_CELL_CRENEAU);
                String compteurValue = AdditionalFieldsSetter.getInstance().getCompteurFieldValue();
                if (compteurValue == null || compteurValue.contentEquals("0") || (precision > selectedSousFamille
                        .getPrecisionGeoCreationMin() && longitude == 0 && latitude == 0)) {
                    new AlertDialog.Builder(SendReportActivity.this).setTitle("Créneau horaire")
                            .setMessage(
                                    "Vous devez renseigner les \"infos complémentaires\" et votre \"localisation\" pour choisir un créneau de rendez-vous.")
                            .setNegativeButton(android.R.string.ok,
                                    null)
                            .show();
                } else {
                    startActivityForResult(TimeSlotChooserActivity.getIntent(SendReportActivity.this,
                            Integer.toString(
                                    selectedType.getIdType()),
                            -1,
                            Integer.parseInt(
                                    compteurValue),
                            selectedId,
                            latitude,
                            longitude,
                            false),
                            SELECT_SLOT_ACTIVITY_REQUEST_CODE);
                }
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        reportActionMasse = null;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!isModifyReport) {
            if (images.size() == 0 && !alreadyAskedPhoto) {
                alreadyAskedPhoto = true;
                openCamera();
            } else if (address == null && !alreadyAskedAddress) {
                alreadyAskedAddress = true;
                openMap();
            }
        }
    }
    @Override
    protected boolean verifyMinPrecision(int precision) {
        return precision <= selectedSousFamille.getPrecisionGeoCreationMin();
    }
    @Override
    protected int getMinPrecision() {
        return selectedSousFamille.getPrecisionGeoCreationMin();
    }
    @Override
    protected boolean isPhotoMandatory() {
        return selectedSousFamille.isPhotoCreationMandatory();
    }
    @Override
    protected void validate() {
        final LocationManager manager = (LocationManager) getSystemService(
                Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !manager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)) {
            buildAlertMessageNoGps();
        } else {
            String toastMessage = "";
            if (images.size() == 0 && selectedSousFamille.isPhotoCreationMandatory()) {
                toastMessage += "La prise de photo est obligatoire.\n";
            }
            if (precision > selectedSousFamille.getPrecisionGeoCreationMin() && longitude == 0 && latitude == 0) {
                toastMessage += "La précision est insuffisante, vous devez valider votre position manuellement.\n";
            }
            if (!AdditionalFieldsSetter.getInstance().mandatoryFieldsAreSetted()) {
                toastMessage += getString(R.string.error_additional_field_mandatory);
            }
            if (toastMessage.equalsIgnoreCase("")) {
                if (isModifyReport && reportActionMasse == null) {
                    // Maj du commentaire
                    ActionSignalement reportActionCom = new ActionSignalement();
                    reportActionCom.setIdSignalement(signalement.getIdSignalement());
                    reportActionCom.setAction(Constants.MOD_DESC_SIGNALEMENT_ACTION);
                    reportActionCom.setType(signalement.getType());
                    Date d = new Date();
                    reportActionCom.setDate(d.getTime());
                    reportActionCom.setAddress(signalement.getAdresse());
                    reportActionCom.setCpAndLocality("" + signalement.getVille().getIdVille());
                    reportActionCom.setIdCreneau(signalement.getPriorite().getIdPriorite());
                    reportActionCom.setDescription(commentaireEditTextS5.getText().toString());
                    DAL.storeActionSignalement(reportActionCom, SendReportActivity.this);
                    Intent signalementComSenderIntent = new Intent(SendReportActivity.this, SignalementSender.class);
                    SendReportActivity.this.startService(signalementComSenderIntent);
                    // Ajout des nouvelles photos
                    if (imagesAdded != null && imagesAdded.size() > 0) {
                        for (String image : imagesAdded) {
                            ActionSignalement reportActionPhotoAdd = new ActionSignalement();
                            reportActionPhotoAdd.setIdSignalement(signalement.getIdSignalement());
                            reportActionPhotoAdd.setAction(Constants.ADD_PHOTO_ACTION);
                            reportActionPhotoAdd.setType(signalement.getType());
                            reportActionPhotoAdd.setDate(new Date().getTime());
                            reportActionPhotoAdd.setPhotos(image);
                            if (selectedId >= 0) {
                                reportActionPhotoAdd.setIdCreneau(selectedId);
                            }
                            DAL.storeActionSignalement(reportActionPhotoAdd, SendReportActivity.this);
                            Intent signalementPhotoAddSenderIntent = new Intent(SendReportActivity.this, SignalementSender.class);
                            SendReportActivity.this.startService(signalementPhotoAddSenderIntent);
                        }
                    }
                    // Supprimer les photos
                    if (imagesDeleted != null && imagesDeleted.size() > 0) {
                        for (String image : imagesDeleted) {
                            String copyImageId = image;
                            ActionSignalement reportActionPhotoDelete = new ActionSignalement();
                            reportActionPhotoDelete.setIdSignalement(signalement.getIdSignalement());
                            reportActionPhotoDelete.setAction(Constants.DELETE_PHOTO_ACTION);
                            reportActionPhotoDelete.setIdPhotoToDelete(copyImageId);
                            reportActionPhotoDelete.setType(signalement.getType());
                            reportActionPhotoDelete.setDate(new Date().getTime());
                            DAL.storeActionSignalement(reportActionPhotoDelete, SendReportActivity.this);
                            Intent signalementPhotoDeleteSenderIntent = new Intent(SendReportActivity.this, SignalementSender.class);
                            SendReportActivity.this.startService(signalementPhotoDeleteSenderIntent);
                        }
                    }
                    // Maj Info complémentaires
                    HashMap<String, String> additionalFieldMap = new HashMap<String, String>();
                    for (ChampComplementaire champComplementaire : AdditionalFieldsSetter.getInstance().getAdditionalFieldList()) {
                        additionalFieldMap.put(Integer.toString(champComplementaire.getId()),
                                AdditionalFieldsSetter.getInstance().getCurrentValue(champComplementaire.getId()));
                    }
                    new UpdateAdditionalFields(
                            getApplicationContext(),
                            signalement.getIdSignalement(),
                            additionalFieldMap,
                            (UpdateAdditionalFields.UpdateAdditionalFieldsListener) stateCode -> {
                                if (stateCode != StateCode.STATE_OK) {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            "Une erreur est survenue dans l'envois des informations complémentaire",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }).downloadAndStoreData();
                    finish();
                    CToast.makeText(SendReportActivity.this, "Modification en cours d'envois", Toast.LENGTH_LONG).show();
                } else {
                    if (reportAction == null) {
                        reportAction = new ActionSignalement();
                    }
                    reportAction.setAction(Constants.CREER_SIGNALEMENT_ACTION);
                    reportAction.setDescription(commentaireEditTextS5.getText().toString());
                    reportAction.setLongitudeMesuree(longitudeMesuree);
                    reportAction.setLatitudeMesuree(latitudeMesuree);
                    if (longitude == 0 || latitude == 0) {
                        longitude = longitudeMesuree;
                        latitude = latitudeMesuree;
                    } else {
                        precision = 1;
                    }
                    reportAction.setLatitude(latitude);
                    reportAction.setLongitude(longitude);
                    reportAction.setPrecision(precision);
                    reportAction.setAddress(address);
                    reportAction.setCpAndLocality(locality);
                    reportAction.setType(selectedType);
                    reportAction.setGroupeCible(targetGroup);
                    Date d = new Date();
                    reportAction.setDate(d.getTime());
                    StringBuilder photosString = new StringBuilder();
                    for (int i = 0; i < images.size(); i++) {
                        photosString.append(images.get(i));
                        if (i < images.size() - 1) {
                            photosString.append(",");
                        }
                    }
                    if (selectedId >= 0) {
                        reportAction.setIdCreneau(selectedId);
                    }
                    reportAction.setPhotos(photosString.toString());
                    reportAction.setDate(new Date().getTime());
                    reportAction.setAdditionalFields((HashMap<Integer, String>) AdditionalFieldsSetter.getInstance().mValues
                            .clone());
                    reportAction.setCanal(SignalementSource.OSIS_MOBILE);
                    if (reportActionMasse != null) {
                        MasseReportMapActivity.currentActionReport = reportAction;
                        reportActionMasse = null;
                        finish();
                    } else {
                        Intent intent = new Intent(SendReportActivity.this, ShowNearbyReportsActivity.class);
                        intent.putExtra(PREDEFINED_LONGITUDE, reportAction.getLongitude());
                        intent.putExtra(PREDEFINED_LATITUDE, reportAction.getLatitude());
                        intent.putExtra(ADDRESS, reportAction.getAddress());
                        startActivityForResult(intent, SEND_REPORT_REQUEST_CODE);
                    }
                }
            } else {
                CToast.makeText(SendReportActivity.this, toastMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected boolean getExtra() {
        if (getIntent().hasExtra(AddCommentActivity.EXTRA_ACTION_REPORT_ID)) {
            reportAction = DAL.getActionSignalementWithIdActionSignalement(getIntent().getIntExtra(
                    AddCommentActivity.EXTRA_ACTION_REPORT_ID,
                    0), getApplicationContext());
            selectedType = reportAction.getType();
            selectedSousFamille = selectedType.sousFamille(getApplicationContext());
            selectedFamilly = selectedSousFamille.famille(getApplicationContext());
            restoreReportAction(reportAction);
        } else if (getIntent().hasExtra(EXTRA_REPORT_ID)) {
            isModifyReport = true;
            String reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
            signalement = DAL.getSignalement(reportId, this);
            selectedType = signalement.getType();
            selectedSousFamille = selectedType.sousFamille(getApplicationContext());
            selectedFamilly = selectedSousFamille.famille(getApplicationContext());
            modifyPhotos = DAL.getPhotosForSignalement(signalement.getIdSignalement(),
                    this);
            for (Photo photo : modifyPhotos) {
                images.add(photo.getNomPhoto());
                listImage.add(Uri.parse(Utils.getBaseUrl(this.getApplicationContext())
                        + this.getString(R.string.PIECE_JOINTE_URL)
                        + String.valueOf(photo.getIdPhoto())
                        + Utils.getAuthentificationParams(this.getApplicationContext())));
            }
            address = signalement.getAdresse();
        } else {
            if(reportActionMasse != null) {
                selectedType = reportActionMasse.getType();
                selectedSousFamille = reportActionMasse.getType().sousFamille(this);
                selectedFamilly = reportActionMasse.getType().sousFamille(this).famille(this);
                address = reportActionMasse.getAddress();
                latitude = reportActionMasse.getLatitude();
                latitudeForcee = latitude;
                longitude = reportActionMasse.getLongitude();
                longitudeForcee = longitude;
            } else {
                int identifiantType = getIntent().getIntExtra("typeId", 0);
                int idSousFamille   = getIntent().getIntExtra("sousFamilleId", 0);
                int idFamilly       = getIntent().getIntExtra("familleId", 0);
                selectedType = DAL.getType(identifiantType, this);
                selectedSousFamille = DAL.getSousFamille(idSousFamille, this);
                selectedFamilly = DAL.getFamille(idFamilly, this);
            }
        }
        AdditionalFieldsSetter.getInstance()
                              .initList(selectedType.getIdType(), getApplicationContext());
        if (getIntent() != null) {
            if (getIntent().hasExtra(PREDEFINED_LONGITUDE)) {
                longitudeForcee = getIntent().getExtras().getDouble("predefined_longitude");
                longitude = longitudeForcee;
            }
            if (getIntent().hasExtra(PREDEFINED_LATITUDE)) {
                latitudeForcee = getIntent().getExtras().getDouble("predefined_latitude");
                latitude = latitudeForcee;
            }
            if (getIntent().hasExtra(ADDRESS)) {
                address = getIntent().getExtras().getString("address");
            }
            if (getIntent().hasExtra(LOCALITY)) {
                locality = getIntent().getExtras().getString("locality");
            }
        }
        return true;
    }
    void restoreReportAction(ActionSignalement reportAction) {
        precision = (int) reportAction.getPrecision();
        latitude = reportAction.getLatitude();
        longitude = reportAction.getLongitude();
        longitudeMesuree = reportAction.getLongitudeMesuree();
        latitudeMesuree = reportAction.getLatitudeMesuree();
    }
    @Override
    protected ArrayList<Groupe> getGroupList() {
        return DAL.getMesGroupesWithCreationRight(getApplicationContext());
    }
    @Override
    protected boolean isCreation() {
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    @Override
    public void updateLonLatPrec(Location location) {
        super.updateLonLatPrec(location);
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_SLOT_ACTIVITY_REQUEST_CODE) {
            handleSlot(requestCode, resultCode, data);
        } else if (requestCode == SEND_REPORT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                SignalementSender.storeActionSignalement(reportAction, SendReportActivity.this);
                Intent signalementSenderIntent = new Intent(SendReportActivity.this,
                        SignalementSender.class);
                startService(signalementSenderIntent);
                Intent intentSignalementsListViewController = new Intent(SendReportActivity.this,
                        MainActivity.class);
                intentSignalementsListViewController.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intentSignalementsListViewController);
                if (Utils.isNetworkAvailable(getApplicationContext())) {
                    toast(getString(R.string.report_sending),ToastColor.SUCCESS, Toast.LENGTH_LONG);
                } else {
                    toast(getString(R.string.report_added),ToastColor.SUCCESS, Toast.LENGTH_LONG);
                }
                reportActionMasse = null;
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    private void handleSlot(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_SLOT_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data.hasExtra("selectedId")) {
                    selectedId = data.getIntExtra("selectedId", -1);
                    if (creneauIdValueTextView != null) {
                        if (selectedId >= 0) {
                            creneauIdValueTextView.setText("OK");
                            creneauIdValueTextView.setTextColor(getResources().getColor(R.color.secondary_color_osis));
                        } else {
                            //creneauIdValueTextView.setText("NOK");
                            //creneauIdValueTextView.setTextColor(getResources().getColor(R.color.red));
                        }
                    }
                }
            }
        }
    }
    @Override
    public void newFamily(int familyId, int subFamilyId, int typeId) {
        selectedType = DAL.getType(typeId, this);
        selectedSousFamille = DAL.getSousFamille(subFamilyId, this);
        selectedFamilly = DAL.getFamille(familyId, this);
        ((TextView) activityView.findViewById(R.id.famillyAndTypeEmptyText)).setVisibility(View.GONE);
        ((LinearLayout) activityView.findViewById(R.id.famillyAndTypeLayoutWithText)).setVisibility(View.VISIBLE);
        ((TextView) activityView.findViewById(R.id.famillyAndTypeText)).setText(selectedFamilly.getLibelleFamille() + " / " + selectedSousFamille.getLibelleSousFamille() + " / " + selectedType.getLibelleType());
        AdditionalFieldsSetter.getInstance().initList(selectedType.getIdType(), getApplicationContext());
        setAdditionnalFieldsAndTimeSlot();
        validerSignalement.setClickable(true);
        validerSignalement.setFocusable(true);
        validerSignalement.setBackgroundColor(getResources().getColor(R.color.secondary_color_osis));
    }
}