/**
 * View Session Repository Tests
 * Location: src/test/java/com/videoanalytics/video/repository/ViewSessionRepositoryTest.java
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ViewSessionRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private ViewSessionRepository viewSessionRepository;

    @Autowired
    private VideoRepository videoRepository;

    private Video testVideo;
    private ViewSession testSession;

    @BeforeEach
    void setUp() {
        testVideo = videoRepository.save(new Video("Test Video", "test-key", Duration.ofMinutes(5), 1L));
        testSession = new ViewSession(testVideo, 1L, "mobile", "iOS", "127.0.0.1");
        testSession = viewSessionRepository.save(testSession);
    }

    @Test
    void whenFindByVideoIdAndUserId_thenReturnSessions() {
        List<ViewSession> sessions = viewSessionRepository.findByVideoIdAndUserId(testVideo.getId(), 1L);
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getDeviceType()).isEqualTo("mobile");
    }

    // Additional tests for other repository methods...
}