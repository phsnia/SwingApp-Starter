#!/usr/bin/env python3
"""
Initialize an SQLite database from an INI configuration, SQL schema, and CSV files.

Default layout:
    project/
        generate.py
        config.ini
        template.sql
        data/
            manga.csv
            users.csv
            posts.csv

CSV filenames (without .csv) must match SQLite table names.
Database IDs are automatically generated as UUIDs.
"""

from __future__ import annotations

import configparser
import csv
import sqlite3
import sys
import uuid

from pathlib import Path, PureWindowsPath
from typing import TextIO


DEFAULT_CONFIG = """\
[database]

driver = sqlite

path = template.sqlite

host = localhost
port = 3306
name = template
username = root
password =

schema = template.sql
foreign_keys = true
timeout = 30


[csv]
directory = data
enabled = true
encoding = utf-8-sig
delimiter = ,
"""

SUPPORTED_DATABASES = {
    "sqlite",
    "mysql",
    "pgsql",
}

class DatabaseInitializer:
    def __init__(self, base_dir: str | Path | None = None) -> None:
        self.base_dir = (
            Path(base_dir)
            if base_dir is not None
            else Path(__file__).resolve().parent
        )

        self.config_path = self.base_dir / "config.ini"
        self.config = configparser.ConfigParser(interpolation=None)
        self.conn: sqlite3.Connection | None = None

    # ------------------------------------------------------------------
    # Configuration
    # ------------------------------------------------------------------

    def config_generate(self) -> None:
        """Create config.ini if it does not already exist."""
        if self.config_path.exists():
            return

        self.base_dir.mkdir(parents=True, exist_ok=True)

        try:
            self.config_path.write_text(
                DEFAULT_CONFIG,
                encoding="utf-8",
            )
        except OSError as error:
            raise RuntimeError("Failed to generate config.ini") from error

        print("[CONFIG] Generated config.ini")

    def config_load(self) -> None:
        self.config_generate()

        loaded = self.config.read(
            self.config_path,
            encoding="utf-8",
        )

        if not loaded:
            raise RuntimeError(
                f"Failed to load config.ini: {self.config_path}"
            )

    def config_value(self, section: str, key: str) -> str:
        if not self.config.has_option(section, key):
            raise RuntimeError(
                f"Missing configuration: {section}.{key}"
            )

        return self.config.get(section, key)

    def config_bool(self, section: str, key: str) -> bool:
        try:
            return self.config.getboolean(section, key)
        except (
            ValueError,
            configparser.NoOptionError,
            configparser.NoSectionError,
        ) as error:
            raise RuntimeError(
                f"Invalid or missing boolean configuration: {section}.{key}"
            ) from error

    def resolve_config_path(self, section: str, key: str) -> Path:
        """Resolve absolute paths as-is and relative paths against base_dir."""
        raw_path = self.config_value(section, key)
        path = Path(raw_path)

        is_windows_absolute = PureWindowsPath(raw_path).is_absolute()

        if path.is_absolute() or is_windows_absolute:
            return path

        return self.base_dir / path

    # ------------------------------------------------------------------
    # Database
    # ------------------------------------------------------------------

    def database_connect(self) -> None:
        db_path = self.resolve_config_path("database", "path")

        timeout = self.config.getfloat("database", "timeout")
        foreign_keys = self.config_bool("database", "foreign_keys")

        db_path.parent.mkdir(parents=True, exist_ok=True)

        try:
            self.conn = sqlite3.connect(
                db_path,
                timeout=timeout,
            )

            self.conn.row_factory = sqlite3.Row

            self.conn.execute(
                f"PRAGMA foreign_keys = "
                f"{'ON' if foreign_keys else 'OFF'}"
            )

        except sqlite3.Error as error:
            raise RuntimeError(
                f"Failed to connect to database: {error}"
            ) from error

        print(f"[DATABASE] Connected: {db_path}")

    def database_apply_schema(self) -> None:
        schema_path = self.resolve_config_path(
            "database",
            "schema",
        )

        if not schema_path.is_file():
            raise RuntimeError(f"Schema not found: {schema_path}")

        try:
            sql = schema_path.read_text(encoding="utf-8")

            assert self.conn is not None
            self.conn.executescript(sql)

        except (OSError, sqlite3.Error) as error:
            raise RuntimeError(
                f"Failed to apply schema: {error}"
            ) from error

        print("[SCHEMA] Applied successfully")

    # ------------------------------------------------------------------
    # Database metadata / SQL identifiers
    # ------------------------------------------------------------------

    def database_tables(self) -> list[str]:
        assert self.conn is not None

        rows = self.conn.execute(
            """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table'
              AND name NOT GLOB 'sqlite_*'
            """
        ).fetchall()

        return [row["name"] for row in rows]

    def database_columns(self, table: str) -> list[str]:
        assert self.conn is not None

        rows = self.conn.execute(
            f"PRAGMA table_info({self.quote_identifier(table)})"
        ).fetchall()

        return [row["name"] for row in rows]

    @staticmethod
    def quote_identifier(identifier: str) -> str:
        """Quote a table or column name."""
        return '"' + identifier.replace('"', '""') + '"'

    # ------------------------------------------------------------------
    # UUID generation
    # ------------------------------------------------------------------

    @staticmethod
    def generate_uuid() -> str:
        """Generate a UUID v4 string."""
        return str(uuid.uuid4())

    # ------------------------------------------------------------------
    # CSV import
    # ------------------------------------------------------------------

    def database_import_csv(self, csv_path: str | Path) -> None:
        csv_path = Path(csv_path)

        file_name = csv_path.name
        table = csv_path.stem

        if table not in self.database_tables():
            print(f"[SKIP] Unknown table: {table}")
            return

        delimiter = self.config_value("csv", "delimiter")
        encoding = self.config_value("csv", "encoding")

        if len(delimiter) != 1:
            raise RuntimeError(
                "CSV delimiter must be exactly one character"
            )

        try:
            with csv_path.open(
                "r",
                newline="",
                encoding=encoding,
            ) as file:
                self._import_csv_stream(
                    file,
                    table,
                    file_name,
                    delimiter,
                )

        except (OSError, UnicodeError, LookupError, csv.Error) as error:
            print(f"[ERROR] Cannot read {csv_path}: {error}")

    def _import_csv_stream(
        self,
        file: TextIO,
        table: str,
        file_name: str,
        delimiter: str,
    ) -> None:
        reader = csv.reader(file, delimiter=delimiter)

        try:
            headers = next(reader, None)
        except csv.Error as error:
            print(f"[ERROR] Invalid CSV {file_name}: {error}")
            return

        if not headers:
            print(f"[SKIP] Empty CSV: {file_name}")
            return

        # Remove UTF-8 BOM if necessary.
        headers[0] = headers[0].removeprefix("\ufeff")

        table_columns = self.database_columns(table)

        # --------------------------------------------------------------
        # Match CSV columns with database columns.
        # --------------------------------------------------------------

        # Exclude id because it will be generated automatically.
        csv_columns = [
            column
            for column in headers
            if column in table_columns and column != "id"
        ]

        if not csv_columns:
            print(f"[SKIP] No matching columns: {file_name}")
            return

        # Keep only the first occurrence of each header.
        unique_columns: list[str] = []
        seen: set[str] = set()

        for column in csv_columns:
            if column not in seen:
                unique_columns.append(column)
                seen.add(column)

        csv_columns = unique_columns

        # --------------------------------------------------------------
        # Add automatically generated UUID.
        # --------------------------------------------------------------

        has_id = "id" in table_columns

        if has_id:
            csv_columns.append("id")

        column_indexes = {
            column: headers.index(column)
            for column in csv_columns
            if column != "id"
        }

        # --------------------------------------------------------------
        # Build INSERT statement.
        # --------------------------------------------------------------

        quoted_table = self.quote_identifier(table)

        quoted_columns = ", ".join(
            self.quote_identifier(column)
            for column in csv_columns
        )

        placeholders = ", ".join(
            "?" for _ in csv_columns
        )

        sql = (
            f"INSERT INTO {quoted_table} ({quoted_columns}) "
            f"VALUES ({placeholders})"
        )

        # --------------------------------------------------------------
        # Import rows.
        # --------------------------------------------------------------

        imported = 0
        failed = 0
        row_number = 1

        assert self.conn is not None

        for row in reader:
            row_number += 1

            values = []

            for column in csv_columns:
                if column == "id":
                    # Generate a new UUID for every database row.
                    values.append(self.generate_uuid())
                    continue

                index = column_indexes[column]

                value = row[index] if index < len(row) else None

                values.append(
                    None if value in ("", None) else value
                )

            try:
                self.conn.execute(sql, values)
                imported += 1

            except sqlite3.Error as error:
                failed += 1

                print(
                    f"[ERROR] {file_name} "
                    f"row {row_number}: {error}"
                )

        print(
            f"[IMPORT] {file_name}: "
            f"{imported} imported, {failed} failed"
        )

    def database_import_all(self) -> None:
        if not self.config_bool("csv", "enabled"):
            return

        csv_dir = self.resolve_config_path(
            "csv",
            "directory",
        )

        if not csv_dir.is_dir():
            print(f"[INFO] CSV directory not found: {csv_dir}")
            return

        csv_files = sorted(csv_dir.glob("*.csv"))

        if not csv_files:
            print("[INFO] No CSV files found")
            return

        for csv_path in csv_files:
            self.database_import_csv(csv_path)

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    def initialize(self) -> None:
        try:
            self.config_load()
            self.database_connect()
            self.database_apply_schema()
            self.database_import_all()

            assert self.conn is not None
            self.conn.commit()

        except Exception:
            if self.conn is not None:
                self.conn.rollback()

            raise

        finally:
            self.close()

        print("[DONE] Database initialization completed")

    def close(self) -> None:
        if self.conn is not None:
            self.conn.close()
            self.conn = None


def main() -> int:
    try:
        database = DatabaseInitializer()
        database.initialize()

        return 0

    except Exception as error:
        print(f"[FATAL] {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())