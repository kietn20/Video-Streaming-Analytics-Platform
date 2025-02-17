/**
 * Video Like Repository Tests
 * Location: src/test/java/com/videoanalytics/video/repository/VideoLikeRepositoryTest.java
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VideoLikeRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private VideoLikeRepository videoLikeRepository;

    @Autowired
    private VideoRepository videoRepository;

    private Video testVideo;
    private VideoLike testLike;

    @BeforeEach
    void setUp() {
        testVideo = videoRepository.save(new Video("Test Video", "test-key", Duration.ofMinutes(5), 1L));
        testLike = new VideoLike(testVideo, 1L);
        testLike = videoLikeRepository.save(testLike);
    }

    @Test
    void whenFindByVideoIdAndUserId_thenReturnLike() {
        Optional<VideoLike> found = videoLikeRepository.findByVideoIdAndUserId(testVideo.getId(), 1L);
        assertThat(found).isPresent();
    }

    @Test
    void whenCountLikesByVideoId_thenReturnCorrectCount() {
        Long count = videoLikeRepository.countLikesByVideoId(testVideo.getId());
        assertThat(count).isEqualTo(1L);
    }

    // Additional tests for other repository methods...
}