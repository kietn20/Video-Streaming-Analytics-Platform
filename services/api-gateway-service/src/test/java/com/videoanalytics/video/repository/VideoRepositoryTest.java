/**
 * Video Repository Tests
 * Location: src/test/java/com/videoanalytics/video/repository/VideoRepositoryTest.java
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VideoRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VideoRepository videoRepository;

    private Video testVideo;

    @BeforeEach
    void setUp() {
        testVideo = new Video("Test Video", "test-storage-key", Duration.ofMinutes(5), 1L);
        testVideo = videoRepository.save(testVideo);
    }

    @Test
    void whenFindByStorageKey_thenReturnVideo() {
        Optional<Video> found = videoRepository.findByStorageKey("test-storage-key");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Video");
    }

    @Test
    void whenIncrementViewCount_thenCountIncreases() {
        videoRepository.incrementViewCount(testVideo.getId());
        Video updated = videoRepository.findById(testVideo.getId()).orElseThrow();
        assertThat(updated.getViewCount()).isEqualTo(1L);
    }

    // Additional tests for other repository methods...
}