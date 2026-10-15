-- Manual SQL Server repair for the six original seed movies.
-- Stop the application before executing this entire script in SSMS.
-- No booking, ticket or showtime data is changed.
USE FUCinemaTicketDB;
GO
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    ALTER TABLE dbo.movies ALTER COLUMN title nvarchar(200) NOT NULL;
    ALTER TABLE dbo.movies ALTER COLUMN director nvarchar(100) NOT NULL;
    ALTER TABLE dbo.movies ALTER COLUMN description nvarchar(2000) NOT NULL;

    -- Match both seed ID and the exact original or code-page-damaged seed
    -- title. Edited titles are skipped. Restore director/description only
    -- when they still equal their original or damaged seed values.
    UPDATE m
    SET title = s.title,
        director = CASE
            WHEN m.director COLLATE Latin1_General_100_BIN2 IN (
                s.director COLLATE Latin1_General_100_BIN2,
                CONVERT(nvarchar(100), CONVERT(varchar(100), s.director COLLATE SQL_Latin1_General_CP1_CI_AS))
                    COLLATE Latin1_General_100_BIN2)
            THEN s.director ELSE m.director END,
        description = CASE
            WHEN m.description COLLATE Latin1_General_100_BIN2 IN (
                s.description COLLATE Latin1_General_100_BIN2,
                CONVERT(nvarchar(2000), CONVERT(varchar(2000), s.description COLLATE SQL_Latin1_General_CP1_CI_AS))
                    COLLATE Latin1_General_100_BIN2)
            THEN s.description ELSE m.description END
    OUTPUT inserted.movie_id, deleted.title AS old_title, inserted.title AS repaired_title
    FROM dbo.movies m
    JOIN (VALUES
        (1, N'Biệt Đội Siêu Tốc', N'Lê Minh'),
        (2, N'Chú Mèo Phiêu Lưu', N'Trần Hà'),
        (3, N'Ngôi Nhà Bí Ẩn', N'Phạm Quân'),
        (4, N'Mùa Hè Năm Ấy', N'Đỗ Lan'),
        (5, N'Vũ Trụ Song Song', N'Ngô Tùng'),
        (6, N'Huyền Thoại Cũ', N'Vũ Bình')
    ) v(movie_id, title, director) ON m.movie_id = v.movie_id
    CROSS APPLY (SELECT v.title, v.director, N'Nội dung phim ' + v.title + N'.' AS description) s
    WHERE m.title COLLATE Latin1_General_100_BIN2 IN (
        s.title COLLATE Latin1_General_100_BIN2,
        CONVERT(nvarchar(200), CONVERT(varchar(200), s.title COLLATE SQL_Latin1_General_CP1_CI_AS))
            COLLATE Latin1_General_100_BIN2);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

SELECT movie_id, title, director FROM dbo.movies ORDER BY movie_id;
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = 'movies'
  AND COLUMN_NAME IN ('title', 'director', 'description');
