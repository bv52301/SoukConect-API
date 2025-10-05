CREATE TABLE Cuisines (
  cuisine_id  BIGINT PRIMARY KEY AUTO_INCREMENT,
  cuisinename VARCHAR(100) NOT NULL,      -- e.g., Asian, Middle East, Mediterranean
  category    VARCHAR(100) NOT NULL,      -- e.g., Indian, Chinese
  subcategory VARCHAR(100) NOT NULL,      -- e.g., South Indian
  region      VARCHAR(100) NOT NULL,      -- e.g., TN, Kerala, Bengali
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_cuisine UNIQUE (cuisinename, category, subcategory, region)
);