package dk.medcom.video.api.service;

import dk.medcom.video.api.controller.exceptions.NotAcceptableException;
import dk.medcom.video.api.controller.exceptions.NotValidDataException;
import dk.medcom.video.api.controller.exceptions.PermissionDeniedException;
import dk.medcom.video.api.controller.exceptions.RessourceNotFoundException;
import dk.medcom.video.api.dao.Organisation;
import dk.medcom.video.api.dao.SchedulingInfo;
import dk.medcom.video.api.dao.SchedulingTemplate;
import dk.medcom.video.api.dto.CreateSchedulingInfoDto;
import dk.medcom.video.api.dto.ProvisionStatus;
import dk.medcom.video.api.helper.TestDataHelper;
import dk.medcom.video.api.repository.OrganisationRepository;
import dk.medcom.video.api.repository.SchedulingInfoRepository;
import dk.medcom.video.api.repository.SchedulingTemplateRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;

public class SchedulingInfoServiceTest {
    private SchedulingInfoRepository schedulingInfoRepository;
    private OrganisationRepository organizationRepository;
    private SchedulingTemplateRepository schedulingTemplateRepository;

    private UUID schedulingInfoUuid;
    private SchedulingInfo schedulingInfo;

    private MeetingUserService meetingUserService;
    private SchedulingTemplateService schedulingTemplateService;

    private static final String NON_POOL_ORG = "nonPoolOrg";
    private static final String POOL_ORG = "poolOrg";

    private static final long SCHEDULING_TEMPLATE_ID = 1L;
    private static final long SCHEDULING_TEMPLATE_ID_OTHER_ORG = 2L;

    @Before
    public void setupMocks() {
        schedulingInfoUuid = UUID.randomUUID();
        schedulingInfo = createSchedulingInfo();

        schedulingInfoRepository = Mockito.mock(SchedulingInfoRepository.class);
        Mockito.when(schedulingInfoRepository.findOneByUuid(schedulingInfoUuid.toString())).thenReturn(schedulingInfo);
        meetingUserService = Mockito.mock(MeetingUserService.class);

        organizationRepository = Mockito.mock(OrganisationRepository.class);
        Mockito.when(organizationRepository.findByOrganisationId(NON_POOL_ORG)).thenReturn(createNonPoolOrganisation());
        Mockito.when(organizationRepository.findByOrganisationId(POOL_ORG)).thenReturn(createOrganisation());

        schedulingTemplateService = Mockito.mock(SchedulingTemplateService.class);

        schedulingTemplateRepository = Mockito.mock(SchedulingTemplateRepository.class);
        Mockito.when(schedulingTemplateRepository.findOne(SCHEDULING_TEMPLATE_ID)).thenReturn(createSchedulingTemplate(SCHEDULING_TEMPLATE_ID));
        Mockito.when(schedulingTemplateRepository.findOne(SCHEDULING_TEMPLATE_ID_OTHER_ORG)).thenReturn(createSchedulingTemplateOtherOrg());

    }


    @Test(expected = RessourceNotFoundException.class)
    public void testUpdateSchedulingInfoNotFound() throws RessourceNotFoundException, PermissionDeniedException {
        SchedulingInfoService schedulingInfoService = new SchedulingInfoService(schedulingInfoRepository, null, null, null, null, null, organizationRepository);

        schedulingInfoService.updateSchedulingInfo(UUID.randomUUID().toString(), new Date());
    }

    @Test
    public void testUpdateSchedulingInfo() throws RessourceNotFoundException, PermissionDeniedException {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.MINUTE, -10);
        Date calculatedStartTime = calendar.getTime();

        SchedulingInfo expectedSchedulingInfo = createSchedulingInfo();
        expectedSchedulingInfo.setvMRStartTime(calculatedStartTime);

        Mockito.when(schedulingInfoRepository.save(Matchers.any(SchedulingInfo.class))).thenReturn(expectedSchedulingInfo);


        SchedulingInfoService schedulingInfoService = new SchedulingInfoService(schedulingInfoRepository, null, null, null, null, meetingUserService, organizationRepository);

        SchedulingInfo schedulingInfo = schedulingInfoService.updateSchedulingInfo(schedulingInfoUuid.toString(), now);

        assertNotNull(schedulingInfo);
        assertEquals(calculatedStartTime, schedulingInfo.getvMRStartTime());

        ArgumentCaptor<SchedulingInfo> schedulingInfoServiceArgumentCaptor = ArgumentCaptor.forClass(SchedulingInfo.class);
        Mockito.verify(schedulingInfoRepository, times(1)).save(schedulingInfoServiceArgumentCaptor.capture());
        SchedulingInfo capturedSchedulingInfo = schedulingInfoServiceArgumentCaptor.getValue();

        assertTrue(calculatedStartTime.equals(capturedSchedulingInfo.getvMRStartTime()));
    }

    @Test
    public void testCreateSchedulingInfoPooling() throws NotValidDataException, PermissionDeniedException, NotAcceptableException {
        DateFormat dateFormat = SimpleDateFormat.getInstance();
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.MINUTE, -10);
        Date calculatedStartTime = calendar.getTime();

        SchedulingInfo expectedSchedulingInfo = createSchedulingInfo();
        expectedSchedulingInfo.setvMRStartTime(calculatedStartTime);

        Mockito.when(schedulingInfoRepository.save(Matchers.any(SchedulingInfo.class))).thenReturn(expectedSchedulingInfo);

        SchedulingInfoService schedulingInfoService = createSchedulingInfoService();

        CreateSchedulingInfoDto input = new CreateSchedulingInfoDto();
        input.setOrganizationId(POOL_ORG);
        input.setTemplateId(SCHEDULING_TEMPLATE_ID);
        input.setProvisionVmrId("vmr id");
        SchedulingInfo schedulingInfo = schedulingInfoService.createSchedulingInfo(input);

        assertNotNull(schedulingInfo);
        assertEquals(calculatedStartTime, schedulingInfo.getvMRStartTime());

        ArgumentCaptor<SchedulingInfo> schedulingInfoServiceArgumentCaptor = ArgumentCaptor.forClass(SchedulingInfo.class);
        Mockito.verify(schedulingInfoRepository, times(1)).save(schedulingInfoServiceArgumentCaptor.capture());
        SchedulingInfo capturedSchedulingInfo = schedulingInfoServiceArgumentCaptor.getValue();

        assertEquals(String.format("Exected start time %s does not match actual start time %s", dateFormat.format(calculatedStartTime), dateFormat.format(capturedSchedulingInfo.getvMRStartTime())), dateFormat.format(calculatedStartTime), dateFormat.format(capturedSchedulingInfo.getvMRStartTime()));
        assertEquals("some theme", capturedSchedulingInfo.getIvrTheme());
//        assertEquals("null/?url=1960@test_domain&pin=4669&start_dato=" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(calculatedStartTime), capturedSchedulingInfo.getPortalLink());
        assertNotNull(capturedSchedulingInfo.getvMRStartTime());
        assertEquals(createOrganisation().getId(), capturedSchedulingInfo.getOrganisation().getId());
        assertEquals(input.getProvisionVmrId(), capturedSchedulingInfo.getProvisionVMRId());
//        assertEquals("", capturedSchedulingInfo.getProvisionTimestamp());
        assertNotNull(capturedSchedulingInfo.getProvisionTimestamp());
        assertEquals("Pooled provisioning OK", capturedSchedulingInfo.getProvisionStatusDescription());
        assertEquals(ProvisionStatus.PROVISIONED_OK, capturedSchedulingInfo.getProvisionStatus());
        assertEquals(1, capturedSchedulingInfo.getSchedulingTemplate().getId().intValue());
//        assertEquals("1960", capturedSchedulingInfo.getUriWithoutDomain());
        assertNotNull(capturedSchedulingInfo.getUriWithoutDomain());
//        assertEquals("1960@test_domain", capturedSchedulingInfo.getUriWithDomain());
        assertNotNull(capturedSchedulingInfo.getUriWithDomain());
        assertEquals(true, capturedSchedulingInfo.getEndMeetingOnEndTime());
        assertEquals(10, capturedSchedulingInfo.getMaxParticipants());
        assertEquals(dateFormat.format(calculatedStartTime), dateFormat.format(capturedSchedulingInfo.getvMRStartTime()));
        assertEquals(10, capturedSchedulingInfo.getVMRAvailableBefore());
        assertNotNull(capturedSchedulingInfo.getGuestPin());
        assertNotNull(capturedSchedulingInfo.getHostPin());
    }

    @Test(expected = NotValidDataException.class)
    public void testCanNotCreateSchedulingInfoOnNonExistingSchedulingTemplate() throws NotValidDataException, PermissionDeniedException, NotAcceptableException {
        CreateSchedulingInfoDto input = new CreateSchedulingInfoDto();
        input.setOrganizationId(POOL_ORG);
        input.setProvisionVmrId("vmr id");
        input.setTemplateId(10L);

        SchedulingInfoService schedulingInfoService = createSchedulingInfoService();

        schedulingInfoService.createSchedulingInfo(input);
    }

    @Test(expected = NotValidDataException.class)
    public void testCanNotCreateSchedulingInfoOnSchedulingTemplateForOtherOrg() throws NotValidDataException, PermissionDeniedException, NotAcceptableException {
        CreateSchedulingInfoDto input = new CreateSchedulingInfoDto();
        input.setOrganizationId(POOL_ORG);
        input.setProvisionVmrId("vmr id");
        input.setTemplateId(SCHEDULING_TEMPLATE_ID_OTHER_ORG);

        SchedulingInfoService schedulingInfoService = createSchedulingInfoService();

        schedulingInfoService.createSchedulingInfo(input);
    }

    @Test(expected = NotValidDataException.class)
    public void testCanNotCreateSchedulingInfoOnNonPoolOrganisation() throws NotValidDataException, PermissionDeniedException, NotAcceptableException {
        CreateSchedulingInfoDto input = new CreateSchedulingInfoDto();
        input.setOrganizationId(NON_POOL_ORG);
        input.setProvisionVmrId("vmr id");
        input.setTemplateId(2L);

        SchedulingInfoService schedulingInfoService = new SchedulingInfoService(schedulingInfoRepository, null, null, null, null, meetingUserService, organizationRepository);

        schedulingInfoService.createSchedulingInfo(input);
    }

    @Test(expected = NotValidDataException.class)
    public void testCanNotCreateSchedulingInfoOnNonExistingOrganisation() throws NotValidDataException, PermissionDeniedException, NotAcceptableException {
        CreateSchedulingInfoDto input = new CreateSchedulingInfoDto();
        input.setOrganizationId("non existing org");
        input.setProvisionVmrId("vmr id");
        input.setTemplateId(2L);

        SchedulingInfoService schedulingInfoService = new SchedulingInfoService(schedulingInfoRepository, null, null, null, null, meetingUserService, organizationRepository);

        schedulingInfoService.createSchedulingInfo(input);
    }

    private SchedulingTemplate createSchedulingTemplate(long id) {
        SchedulingTemplate schedulingTemplate = new SchedulingTemplate();
        schedulingTemplate.setId(id);
        schedulingTemplate.setUriNumberRangeLow(1000L);
        schedulingTemplate.setUriNumberRangeHigh(2000L);
        schedulingTemplate.setUriPrefix("uri_prefix");
        schedulingTemplate.setUriDomain("test_domain");
        schedulingTemplate.setVMRAvailableBefore(10);
        schedulingTemplate.setMaxParticipants(10);
        schedulingTemplate.setIvrTheme("some theme");
        schedulingTemplate.setHostPinRequired(true);
        schedulingTemplate.setHostPinRangeLow(10000L);
        schedulingTemplate.setHostPinRangeHigh(20000L);
        schedulingTemplate.setGuestPinRequired(true);
        schedulingTemplate.setGuestPinRangeLow(4000L);
        schedulingTemplate.setGuestPinRangeHigh(5000L);
        schedulingTemplate.setEndMeetingOnEndTime(true);
        schedulingTemplate.setConferencingSysId(1111L);
        schedulingTemplate.setIsDefaultTemplate(false);

        return schedulingTemplate;
    }

    private SchedulingTemplate createSchedulingTemplateOtherOrg() {
        SchedulingTemplate schedulingTemplate = createSchedulingTemplate(SCHEDULING_TEMPLATE_ID_OTHER_ORG);
        schedulingTemplate.setOrganisation(createOrganisation(false, "some org id", 10L));

        return schedulingTemplate;
    }

    public SchedulingInfo createSchedulingInfo() {
        SchedulingInfo schedulingInfo = new SchedulingInfo();
        schedulingInfo.setVMRAvailableBefore(10);
        schedulingInfo.setUuid(schedulingInfoUuid.toString());
        schedulingInfo.setOrganisation(createOrganisation());

        return schedulingInfo;
    }

    private SchedulingInfoService createSchedulingInfoService() {
        return new SchedulingInfoService(schedulingInfoRepository, schedulingTemplateRepository, schedulingTemplateService, null, null, meetingUserService, organizationRepository);
    }

    private Organisation createNonPoolOrganisation()  {
        return createOrganisation(false, NON_POOL_ORG, 2);
    }

    private Organisation createOrganisation(boolean poolEnabled, String orgId, long id)  {
        return TestDataHelper.createOrganisation(poolEnabled, orgId, id);
    }

    private Organisation createOrganisation() {
        return createOrganisation(true, POOL_ORG, 1);
    }
}