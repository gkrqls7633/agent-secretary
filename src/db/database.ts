import sqlite3 from 'sqlite3';
import { open, Database } from 'sqlite';
import path from 'path';

export class DatabaseManager {
  private db: Database | null = null;
  private dbPath: string;

  constructor() {
    // 프로젝트 루트의 db 폴더에 저장
    this.dbPath = path.join(process.cwd(), 'db', 'secretary.db');
  }

  /**
   * 데이터베이스를 초기화하고 필요한 테이블을 생성합니다.
   */
  async init() {
    this.db = await open({
      filename: this.dbPath,
      driver: sqlite3.Database
    });

    // 상호작용 로그 테이블 생성
    await this.db.exec(`
      CREATE TABLE IF NOT EXISTS interaction_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
        scenario TEXT NOT NULL, -- morning, focus, restoration
        proposal_text TEXT,
        user_action TEXT, -- accepted, rejected, snoozed
        rejection_reason TEXT,
        focus_score INTEGER,
        context_data TEXT -- 날씨, 에너지 등 당시의 컨텍스트를 JSON으로 저장
      )
    `);

    console.log('📦 Local Database (SQLite) initialized at', this.dbPath);
    return this.db;
  }

  /**
   * 상호작용 로그를 기록합니다.
   */
  async logInteraction(data: {
    scenario: string;
    proposal_text: string;
    user_action: string;
    rejection_reason?: string;
    focus_score?: number;
    context_data?: any;
  }) {
    if (!this.db) throw new Error('DB not initialized');

    await this.db.run(
      `INSERT INTO interaction_logs 
      (scenario, proposal_text, user_action, rejection_reason, focus_score, context_data) 
      VALUES (?, ?, ?, ?, ?, ?)`,
      [
        data.scenario,
        data.proposal_text,
        data.user_action,
        data.rejection_reason || null,
        data.focus_score || null,
        JSON.stringify(data.context_data || {})
      ]
    );
  }

  /**
   * 특정 시나리오의 최근 로그를 가져옵니다. (학습용)
   */
  async getRecentLogs(scenario: string, limit: number = 5) {
    if (!this.db) throw new Error('DB not initialized');
    return this.db.all(
      'SELECT * FROM interaction_logs WHERE scenario = ? ORDER BY timestamp DESC LIMIT ?',
      [scenario, limit]
    );
  }
}
